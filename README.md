Simple Bank – Web Application
Description
The developed web application, Simple Bank, provides core banking functionalities for three distinct user roles: Customers, Staff, and Managers. Each role interacts with a dedicated web portal tailored to their specific tasks.

This project emphasizes the use of:

Java Servlets for server-side logic and request handling

JavaServer Pages (JSP), JSTL (JSP Standard Tag Library), and Expression Language (EL) for dynamic HTML generation

JDBC (Java Database Connectivity) for data persistence

SQLite as the backend database to store user accounts, transactions, loans, fixed deposits, and grievances

Apache Tomcat as the servlet container

SHA-256 for password hashing and security

Application Functionality
Customer Portal
Login with account number and password

View account dashboard: balance and transaction history

Initiate fund transfers to other customer accounts

Manage Fixed Deposits (FDs): request new FDs, view existing FDs, and close active FDs with interest calculation

Manage Loans: request new loans, view loan status (pending, active, repaid), repay active loans with interest

Submit grievances and view their status

Change account password

Logout

Staff Portal
Login with staff mobile number and password

View personal dashboard

Submit new customer signup requests for manager approval

Process customer deposits and withdrawals (subject to balance checks)

Update customer email (address update not supported due to schema design)

Change personal password

Logout

Manager Portal
Login with manager mobile number and password

View personal dashboard

Create new staff accounts

Approve or reject pending:

Customer signup requests

Fixed Deposit (FD) requests with interest rate setting

Loan requests with interest rate setting

Create loans directly for customers

View and update the status and remarks of customer grievances

Update personal details (name, mobile number)

Change personal password

Logout

Source Code Structure and Packages
The Java source code is located in the src directory and follows a modular design based on the Model-View-Controller (MVC) pattern adapted for Servlet-based applications.

db Package
Handles data persistence:

DatabaseUtil.java: Utility for establishing JDBC connections to the simple_bank.db SQLite database and safely closing resources

CustomerDB.java, ManagerDB.java, StaffDB.java: Contain static methods for performing CRUD operations specific to their respective user roles

All classes use JDBC PreparedStatement for executing SQL queries

model Package
Contains Plain Old Java Objects (POJOs) representing application data entities:

Entities include Customer, Loan, FD, Grievance, PartialSignup, Staff, Transaction, etc.

Fields are private with public getter methods

JSPs use these model objects with EL (Expression Language) to dynamically display data

servlet Package
Contains all servlet classes, organized into sub-packages by user role:

servlet.customer.*, servlet.staff.*, servlet.manager.*

Each sub-package includes a main Router Servlet (e.g., CustomerRouter.java) that extends HttpServlet

URL mappings are defined in web.xml (e.g., /customer/*)

The Router delegates the request to static handle(request, response) methods within specific handler classes (e.g., LoginServlet.java, DashboardServlet.java, CustomerFDServlet.java)

Handler methods:

Authenticate requests using session attributes

Retrieve or process data by calling methods in the db package

Set request attributes for display

Forward requests to JSP files for HTML rendering

Web Content Structure
JSP files are located in WEB-INF/pages

Static assets such as CSS and images are stored within the main web application directory

URL-to-servlet mappings and other application configurations are defined in the web.xml file
