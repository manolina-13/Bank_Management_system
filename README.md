# Simple Bank_Management_system
\vspace{-3mm}
\subsection*{Description}
The developed web application, "Simple Bank," provides core banking functionalities for three distinct user roles: Customers, Staff, and Managers. Each role interacts with a dedicated web portal tailored to their specific tasks. The project emphasizes the use of \textbf{Java Servlets} for server-side logic and request handling. \textbf{JavaServer Pages (JSP)} along with \textbf{JSTL (JSP Standard Tag Library)} and \textbf{Expression Language (EL)} are employed for dynamically generating HTML views presented to the user. Data persistence is achieved using \textbf{JDBC (Java Database Connectivity)} to interact with an \textbf{SQLite} database, which stores all application data including user accounts, transactions, loans, fixed deposits, and grievances. The application is deployed and runs on an \textbf{Apache Tomcat} server, which serves as the servlet container. Password hashing for security is implemented using SHA-256.

\vspace{-3mm}
\subsection*{Application Functionality}
The application's features are segregated based on user roles:
\begin{itemize}
    \item \textbf{Customer Portal:}
    \begin{itemize}
        \item Login with account number and password.
        \item View account dashboard: balance, transaction history.
        \item Initiate fund transfers to other customer accounts.
        \item Manage Fixed Deposits (FDs): request new FDs, view existing FDs, and close active FDs (with interest calculation based on tenure).
        \item Manage Loans: request new loans, view existing loans (pending, active, repaid), and repay active loans (with interest calculation).
        \item Submit grievances and view their status.
        \item Change account password.
        \item Logout.
    \end{itemize}
    \item \textbf{Staff Portal:}
    \begin{itemize}
        \item Login with staff mobile number and password.
        \item View staff dashboard with personal details.
        \item Submit new customer signup requests for manager approval.
        \item Process customer deposits and withdrawals (subject to balance checks).
        \item Update customer email. (Address update is noted as not supported due to schema).
        \item Change personal password.
        \item Logout.
    \end{itemize}
    \item \textbf{Manager Portal:}
    \begin{itemize}
        \item Login with manager mobile number and password.
        \item View manager dashboard with personal details.
        \item Create new staff accounts.
        \item Approve or reject pending customer signup requests.
        \item Approve or reject pending Fixed Deposit (FD) requests, setting interest rates.
        \item Approve or reject pending Loan requests, setting interest rates.
        \item Create loans directly for customers.
        \item View and update the status/remarks of customer grievances.
        \item Update personal details (name, mobile number).
        \item Change personal password.
        \item Logout.
    \end{itemize}
\end{itemize}


\vspace{-3mm}
\subsection*{Source Code Structure and Packages}
The Java source code (located in the \texttt{src} directory) is organized into packages to promote a modular and maintainable structure, loosely following a Model-View-Controller (MVC) pattern adapted for a Servlet-based application.
The web application's entry point is configured in \texttt{web.xml}, which maps URL patterns (e.g., \texttt{/customer/*}, \texttt{/staff/*}, \texttt{/manager/*}) to specific Router Servlets. These Routers then delegate requests to handler classes or methods based on the specific path.

The primary packages are:
\begin{itemize}
    \item \textbf{db (\texttt{db.CustomerDB}, \texttt{db.ManagerDB}, \texttt{db.StaffDB}, \texttt{db.DatabaseUtil})} : This package is central to data persistence. \texttt{DatabaseUtil.java} provides a utility for establishing JDBC connections to the SQLite database (\texttt{simple\_bank.db}) and for quietly closing resources. \texttt{CustomerDB.java}, \texttt{ManagerDB.java}, and \texttt{StaffDB.java} contain static methods that encapsulate all database operations (CRUD - Create, Read, Update, Delete) specific to their respective user roles or entities. This includes operations for customer accounts, staff accounts, transactions, loans, FDs, and grievances. These classes use JDBC \texttt{PreparedStatement} for executing SQL queries.

    \item \textbf{model (\texttt{model.Customer}, \texttt{model.Staff}, \texttt{model.Transaction}, etc.)} : This package comprises Plain Old Java Objects (POJOs) representing the application's data entities (e.g., \texttt{Customer}, \texttt{Loan}, \texttt{FD}, \texttt{Grievance}, \texttt{PartialSignup}, \texttt{Staff}, \texttt{Transaction}). These model classes have private fields and public getter methods. They are used to transfer data between the database layer, servlet layer, and the JSP view layer. JSPs access data from these model objects using Expression Language (EL).
    \item \textbf{servlet (\texttt{servlet.customer.*}, \texttt{servlet.manager.*}, \texttt{servlet.staff.*})} : This package contains all the servlet classes, organized into sub-packages for each user portal.
    \begin{itemize}
        \item Each sub-package (e.g., \texttt{servlet.customer}) has a main Router Servlet (e.g., \texttt{CustomerRouter.java}) that extends \texttt{HttpServlet}. This Router Servlet intercepts requests based on URL patterns defined in \texttt{web.xml} (e.g., \texttt{/customer/*}).
        \item The Router Servlet then dispatches the request to static \texttt{handle(request, response)} methods within specific "handler" classes (e.g., \texttt{LoginServlet.java}, \texttt{DashboardServlet.java}, \texttt{CustomerFDServlet.java}) based on the request's sub-path (\texttt{request.getPathInfo()}).
        \item These handler methods perform authentication checks (verifying session attributes), retrieve or process data by calling methods in the \texttt{db} package, set request attributes with data to be displayed, and finally forward the request to the appropriate JSP page (located in \texttt{WEB-INF/pages/...}) for rendering the HTML response.
    \end{itemize}
\end{itemize}
The web content, including JSP files, CSS, and any images, is organized under the \texttt{simple-bank} web application directory, with JSPs in \texttt{WEB-INF/pages}.
