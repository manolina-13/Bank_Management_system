# Simple Bank Web Application

## Description

The developed web application, "Simple Bank," provides core banking functionalities for three distinct user roles: Customers, Staff, and Managers. Each role interacts with a dedicated web portal tailored to their specific tasks. The project emphasizes the use of **Java Servlets** for server-side logic and request handling. **JavaServer Pages (JSP)** along with **JSTL (JSP Standard Tag Library)** and **Expression Language (EL)** are employed for dynamically generating HTML views presented to the user. Data persistence is achieved using **JDBC (Java Database Connectivity)** to interact with an **SQLite** database, which stores all application data including user accounts, transactions, loans, fixed deposits, and grievances. The application is deployed and runs on an **Apache Tomcat** server, which serves as the servlet container. Password hashing for security is implemented using SHA-256.

## Application Functionality

The application's features are segregated based on user roles:

### Customer Portal:

*   Login with account number and password.
*   View account dashboard: balance, transaction history.
*   Initiate fund transfers to other customer accounts.
*   Manage Fixed Deposits (FDs): request new FDs, view existing FDs, and close active FDs (with interest calculation based on tenure).
*   Manage Loans: request new loans, view existing loans (pending, active, repaid), and repay active loans (with interest calculation).
*   Submit grievances and view their status.
*   Change account password.
*   Logout.

### Staff Portal:

*   Login with staff mobile number and password.
*   View staff dashboard with personal details.
*   Submit new customer signup requests for manager approval.
*   Process customer deposits and withdrawals (subject to balance checks).
*   Update customer email. (Address update is noted as not supported due to schema).
*   Change personal password.
*   Logout.

### Manager Portal:

*   Login with manager mobile number and password.
*   View manager dashboard with personal details.
*   Create new staff accounts.
*   Approve or reject pending customer signup requests.
*   Approve or reject pending Fixed Deposit (FD) requests, setting interest rates.
*   Approve or reject pending Loan requests, setting interest rates.
*   Create loans directly for customers.
*   View and update the status/remarks of customer grievances.
*   Update personal details (name, mobile number).
*   Change personal password.
*   Logout.

## Source Code Structure and Packages

The Java source code (located in the `src` directory) is organized into packages to promote a modular and maintainable structure, loosely following a Model-View-Controller (MVC) pattern adapted for a Servlet-based application.
The web application's entry point is configured in `web.xml`, which maps URL patterns (e.g., `/customer/*`, `/staff/*`, `/manager/*`) to specific Router Servlets. These Routers then delegate requests to handler classes or methods based on the specific path.

The primary packages are:

*   **db (`db.CustomerDB`, `db.ManagerDB`, `db.StaffDB`, `db.DatabaseUtil`):** This package is central to data persistence. `DatabaseUtil.java` provides a utility for establishing JDBC connections to the SQLite database (`simple_bank.db`) and for quietly closing resources. `CustomerDB.java`, `ManagerDB.java`, and `StaffDB.java` contain static methods that encapsulate all database operations (CRUD - Create, Read, Update, Delete) specific to their respective user roles or entities. This includes operations for customer accounts, staff accounts, transactions, loans, FDs, and grievances. These classes use JDBC `PreparedStatement` for executing SQL queries.

*   **model (`model.Customer`, `model.Staff`, `model.Transaction`, etc.):** This package comprises Plain Old Java Objects (POJOs) representing the application's data entities (e.g., `Customer`, `Loan`, `FD`, `Grievance`, `PartialSignup`, `Staff`, `Transaction`). These model classes have private fields and public getter methods. They are used to transfer data between the database layer, servlet layer, and the JSP view layer. JSPs access data from these model objects using Expression Language (EL).

*   **servlet (`servlet.customer.*`, `servlet.manager.*`, `servlet.staff.*`):** This package contains all the servlet classes, organized into sub-packages for each user portal.

    *   Each sub-package (e.g., `servlet.customer`) has a main Router Servlet (e.g., `CustomerRouter.java`) that extends `HttpServlet`. This Router Servlet intercepts requests based on URL patterns defined in `web.xml` (e.g., `/customer/*`).

    *   The Router Servlet then dispatches the request to static `handle(request, response)` methods within specific "handler" classes (e.g., `LoginServlet.java`, `DashboardServlet.java`, `CustomerFDServlet.java`) based on the request's sub-path (`request.getPathInfo()`).

    *   These handler methods perform authentication checks (verifying session attributes), retrieve or process data by calling methods in the `db` package, set request attributes with data to be displayed, and finally forward the request to the appropriate JSP page (located in `WEB-INF/pages/...`) for rendering the HTML response.

The web content, including JSP files, CSS, and any images, is organized under the `simple-bank` web application directory, with JSPs in `WEB-INF/pages`.
