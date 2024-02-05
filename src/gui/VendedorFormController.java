package gui;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import db.DbException;
import gui.listeners.DataChangeListener;
import gui.util.Alerts;
import gui.util.Constraints;
import gui.util.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import model.entities.Departamento;
import model.entities.Vendedor;
import model.exceptions.ValidationException;
import model.services.DepartamentoService;
import model.services.VendedorService;

public class VendedorFormController implements Initializable {

	private Vendedor entity;

	private VendedorService service;

	private DepartamentoService departamentoService;

	private List<DataChangeListener> dataChangeListeners = new ArrayList<>();

	@FXML
	private TextField txtId;

	@FXML
	private TextField txtName;

	@FXML
	private TextField txtEmail;

	@FXML
	private TextField txtBaseSalary;

	@FXML
	private DatePicker dpBirthDate;

	@FXML
	private ComboBox<Departamento> comboBoxDepartamento;

	@FXML
	private Label labelErrorName;

	@FXML
	private Label labelErrorEmail;

	@FXML
	private Label labelErrorBirthDate;

	@FXML
	private Label labelErrorBaseSalary;

	@FXML
	private Button btSave;

	@FXML
	private Button btCancel;

	private ObservableList<Departamento> obsList;

	public void setVendedor(Vendedor entity) {
		this.entity = entity;
	}

	public void setServices(VendedorService service, DepartamentoService departamentoService) {
		this.service = service;
		this.departamentoService = departamentoService;
	}

	public void subscribeDataChangeListener(DataChangeListener listener) {
		dataChangeListeners.add(listener);
	}

	@FXML
	public void onBtSaveAction(ActionEvent event) {
		if (entity == null) {
			throw new IllegalStateException("Entity está null");
		}
		if (service == null) {
			throw new IllegalStateException("Service está null");
		}
		try {
			entity = getFormData();
			service.saveOrUpdate(entity);
			notifyDataChangeListeners();
			Utils.currentStage(event).close();
		} catch (ValidationException e) {
			setErrorMessages(e.getErrors());
		} catch (DbException e) {
			Alerts.showAlerts("Erro salvando obj", null, e.getMessage(), AlertType.ERROR);
		}

	}

	private void notifyDataChangeListeners() {
		for (DataChangeListener listener : dataChangeListeners) {
			listener.onDataChanged();
		}
	}

	private Vendedor getFormData() {
		Vendedor obj = new Vendedor();

		ValidationException exception = new ValidationException("Validation error");

		obj.setId(Utils.tryParseToInt(txtId.getText()));

		if (txtName.getText() == null || txtName.getText().trim().equals("")) {
			exception.addError("name", "campo não pode estar vazio");
		}
		obj.setName(txtName.getText());
		
		if (txtEmail.getText() == null || txtEmail.getText().trim().equals("")) {
			exception.addError("email", "campo não pode estar vazio");
		}
		obj.setEmail(txtEmail.getText());
		
		if (dpBirthDate.getValue() == null) {
			exception.addError("birthDate", "campo não pode estar vazio");
		}
		else {
			Instant instant = Instant.from(dpBirthDate.getValue().atStartOfDay(ZoneId.systemDefault()));
			obj.setBirthDate(Date.from(instant));
		}
		
		if (txtBaseSalary.getText() == null || txtBaseSalary.getText().trim().equals("")) {
			exception.addError("baseSalary", "campo não pode estar vazio");
		}
		obj.setBaseSalary(Utils.tryParseToDouble(txtBaseSalary.getText()));

		obj.setDepartment(comboBoxDepartamento.getValue());
		
		if (exception.getErrors().size() > 0) {
			throw exception;
		}

		return obj;
	}

	@FXML
	public void onBtCancelAction(ActionEvent event) {
		Utils.currentStage(event).close();
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		initializeNodes();
	}

	private void initializeNodes() {
		Constraints.setTextFieldInteger(txtId);
		Constraints.setTextFieldMaxLength(txtName, 70);
		Constraints.setTextFieldDouble(txtBaseSalary);
		Constraints.setTextFieldMaxLength(txtBaseSalary, 60);
		Utils.formatDatePicker(dpBirthDate, "dd/MM/yyyy");
		
		initializeComboBoxDepartamento();
	}

	public void updateFormData() {
		if (entity == null) {
			throw new IllegalStateException("Entity está null");
		}
		txtId.setText(String.valueOf(entity.getId()));
		txtName.setText(entity.getName());
		txtEmail.setText(entity.getEmail());
		Locale.setDefault(Locale.US);
		txtBaseSalary.setText(String.format("%.2f", entity.getBaseSalary()));
		if (entity.getBirthDate() != null) {
			dpBirthDate.setValue(LocalDate.ofInstant(entity.getBirthDate().toInstant(), ZoneId.systemDefault()));
		}
		if (entity.getDepartment() == null) {
			comboBoxDepartamento.getSelectionModel().selectFirst();
		}
		comboBoxDepartamento.setValue(entity.getDepartment());
	}

	public void loadAssociatedObjects() {
		if (departamentoService == null) {
			throw new IllegalStateException("DepartamentoService está null");
		}
		List<Departamento> list = departamentoService.findAll();
		obsList = FXCollections.observableArrayList(list);
		comboBoxDepartamento.setItems(obsList);
	}

	private void setErrorMessages(Map<String, String> errors) {
		Set<String> fields = errors.keySet();

		labelErrorName.setText(fields.contains("name") ? errors.get("name") : "");
		labelErrorEmail.setText(fields.contains("email") ? errors.get("email") : "");
		labelErrorBirthDate.setText(fields.contains("birthDate") ? errors.get("birthDate") : "");
		labelErrorBaseSalary.setText(fields.contains("baseSalary") ? errors.get("baseSalary") : "");
	}

	private void initializeComboBoxDepartamento() {
		Callback<ListView<Departamento>, ListCell<Departamento>> factory = lv -> new ListCell<Departamento>() {
			@Override
			protected void updateItem(Departamento item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? "" : item.getName());
			}
		};
		comboBoxDepartamento.setCellFactory(factory);
		comboBoxDepartamento.setButtonCell(factory.call(null));
	}

}
