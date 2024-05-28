package ru.mai.khasanov.cipherchat.vaadin.view.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
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

@Route("")
@PageTitle("Login")
public class LoginView extends VerticalLayout {
    @Autowired
    public LoginView(AuthService authService) {
        addClassName("login-view");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Welcome");

        TextField usernameField = new TextField("Username");
        PasswordField passwordField = new PasswordField("Password");

        Button loginButton = new Button("Login", event -> {
            try {
                User user = authService.authenticate(usernameField.getValue(), passwordField.getValue());
                Notification.show("Authentication successful");

                navigateToMainView(user);

            } catch (AuthService.AuthException e) {
                Notification.show("Wrong credentials.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
                Notification.show("Something wrong");
            }
        });

        Button registerButton = new Button("Sing up", event -> UI.getCurrent().navigate(RegisterView.class));

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.add(loginButton, registerButton);

        add(title, usernameField, passwordField, buttonBar);
    }

    private void navigateToMainView(User user) {
        UI.getCurrent().navigate(MainView.class, new RouteParameters("userId", String.valueOf(user.getId())));
    }
}
