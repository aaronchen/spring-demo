package cc.desuka.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import jakarta.servlet.Servlet;
import java.sql.SQLException;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner startH2WebServer() {
		return args -> {
			try {
				org.h2.tools.Server webServer = org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort",
						"8082");
				webServer.start();
				System.out.println("H2 web server started on port 8082 (browse http://127.0.0.1:8082)");
			} catch (SQLException e) {
				System.err.println("Failed to start H2 web server: " + e.getMessage());
			}
		};
	}

	@Bean
	public ServletRegistrationBean<?> h2ConsoleServlet() {
		try {
			// Prefer the Jakarta servlet class (compatible with Spring Boot 4 / Jakarta)
			Class<?> servletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
			Servlet servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();
			ServletRegistrationBean<Servlet> reg = new ServletRegistrationBean<>(servlet, "/h2-console/*");
			reg.setName("H2Console");
			return reg;
		} catch (ClassNotFoundException e) {
			// Jakarta servlet class not present: avoid loading legacy javax-based
			// WebServlet
			// Register a safe placeholder servlet that returns 404 and logs a message.
			jakarta.servlet.http.HttpServlet placeholder = new jakarta.servlet.http.HttpServlet() {
				@Override
				protected void doGet(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse resp)
						throws java.io.IOException {
					resp.sendError(404, "H2 console servlet not available in this runtime (no Jakarta H2 servlet)");
				}
			};
			ServletRegistrationBean<jakarta.servlet.http.HttpServlet> reg = new ServletRegistrationBean<>(placeholder,
					"/h2-console/*");
			reg.setName("H2ConsolePlaceholder");
			return reg;
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to register H2 console servlet", ex);
		}
	}

}
