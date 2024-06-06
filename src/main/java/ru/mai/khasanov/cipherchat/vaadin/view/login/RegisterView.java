package ru.mai.khasanov.cipherchat.vaadin.view.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.service.AuthService;
import ru.mai.khasanov.cipherchat.vaadin.view.chat.MainView;

@Route("register")
@PageTitle("Signup")
public class RegisterView extends VerticalLayout {
    private final AuthService authService;

    @Autowired
    public RegisterView(AuthService authService) {
        this.authService = authService;

        addClassName("login-view");

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H1 title = new H1("Register");

        TextField usernameField = new TextField("Username");
        PasswordField passwordField = new PasswordField("Password");
        PasswordField confirmPasswordField = new PasswordField("Confirm Password");

        Button backButton = new Button(VaadinIcon.BACKSPACE.create(), event -> navigateToLoginView());

        Button submitButton = new Button("Register", event -> register(
                usernameField.getValue(),
                passwordField.getValue(),
                confirmPasswordField.getValue())
        );

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.add(backButton, submitButton);

        add(title, usernameField, passwordField, confirmPasswordField, buttonBar);
    }

    private void register(String username, String password, String confirmPassword) {
        if (username.trim().isEmpty()) {
            Notification.show("Enter a username");
        } else if (password.isEmpty()) {
            Notification.show("Enter a password");
        } else if (!password.equals(confirmPassword)) {
            Notification.show("Passwords don't match");
        } else {
            User user = authService.register(username, password);
            Notification.show("Registration succeeded");

            navigateToMainView(user);
        }
    }

    private void navigateToLoginView() {
        UI.getCurrent().navigate(LoginView.class);
    }

    private void navigateToMainView(User user) {
        UI.getCurrent().navigate(MainView.class, new RouteParameters("userId", String.valueOf(user.getId())));
    }

}
