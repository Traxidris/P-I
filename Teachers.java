import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Teachers {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/", new MyHandler());
        server.createContext("/update", new UpdateHandler());
        server.createContext("/delete/", new DeleteHandler());
        server.createContext("/add", new AddHandler());
        server.createContext("/addSubmit", new AddSubmitHandler());
        server.createContext("/search", new SearchHandler());
        server.createContext("/searchResults", new SearchResultsHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8081");
    }
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<html><head><title>Teachers List</title></head><body>" +
                    "<h2>Teachers List</h2>" +
                    "<form action=\"/search\" method=\"get\">" +
                    "Search by Surname: <input type=\"text\" name=\"surname\">" +
                    "<input type=\"submit\" value=\"Search\" class=\"search-btn\">" +
                    "</form>" +
                    getDataFromDatabase() +
                    "</body></html>";
            sendResponse(t, response);
        }
    }
    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String request = t.getRequestURI().getQuery();
            String[] queryParams = request.split("=");
            if (queryParams.length > 1 && queryParams[1] != null && !queryParams[1].isEmpty()) {
                String surname = queryParams[1];
                t.getResponseHeaders().set("Location", "/searchResults?surname=" + surname);
            } else {
                t.getResponseHeaders().set("Location", "/");
            }
            t.sendResponseHeaders(302, -1);
        }
    }
    static class SearchResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String request = t.getRequestURI().getQuery();
            String surname = request.split("=")[1];
            String response = searchFromDatabase(surname);
            sendResponse(t, response);
        }
    }
    static class UpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                if (query != null) {
                    updateDataInDatabase(query);
                } else {
                    System.err.println("Received null query from POST request.");
                }
                t.getResponseHeaders().set("Location", "/");
                t.sendResponseHeaders(302, -1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    static class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String path = t.getRequestURI().getPath();
                String[] pathSegments = path.split("/");
                if (pathSegments.length == 3) {
                    String idString = pathSegments[2];
                    int id = Integer.parseInt(idString);
                    deleteDataFromDatabase(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            t.getResponseHeaders().set("Location", "/");
            t.sendResponseHeaders(302, -1);
        }
        private void deleteDataFromDatabase(int id) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/university", "root", "");
                String deleteQuery = "delete from teachers where id=?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                    preparedStatement.setInt(1, id);
                    preparedStatement.executeUpdate();
                }
                connection.close();
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    static class AddHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = getAddForm();
            t.getResponseHeaders().set("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
        private String getAddForm() {
            String htmlFilePath = "src\\html\\Teachers.html";
            try {
                String htmlContent = new String(Files.readAllBytes(Paths.get(htmlFilePath)));
                return htmlContent;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error: Unable to read HTML file";
            }
        }
    }
    static class AddSubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String formData = br.readLine();
                if (formData != null) {
                    addDataToDatabase(formData);
                } else {
                    System.err.println("Received null data from POST request.");
                }
                t.getResponseHeaders().set("Location", "/");
                t.sendResponseHeaders(302, -1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private void addDataToDatabase(String formData) {
            try {
                String[] params = formData.split("&");
                String name = params[0].split("=")[1];
                String surname = params[1].split("=")[1];
                int age = Integer.parseInt(params[2].split("=")[1]);
                String gender = params[3].split("=")[1];
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/university", "root", "");
                String insertQuery = "insert into teachers (name, surname, age, gender) values (?, ?, ?, ?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                    preparedStatement.setString(1, name);
                    preparedStatement.setString(2, surname);
                    preparedStatement.setInt(3, age);
                    preparedStatement.setString(4, gender);
                    preparedStatement.executeUpdate();
                }
                connection.close();
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static String searchFromDatabase(String surname) {
        StringBuilder result = new StringBuilder();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/university", "root", "");
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM students WHERE surname LIKE ?");
            preparedStatement.setString(1, "%" + surname + "%");
            ResultSet resultSet = preparedStatement.executeQuery();
            result.append("<html><head>")
                    .append("<style>")
                    .append("body { font-family: Arial, sans-serif; margin: 20px; padding-left: 50px; padding-right: 50px; background-color: #000000; color: #ffffff; border-radius: 20px; }")
                    .append("table { border-collapse: collapse; width: 100%; background-color: #333333; color: #ffffff; border-radius: 20px; }")
                    .append("th, td { border: 1px solid #dddddd; text-align: left; padding: 10px; border-radius: 20px; }")
                    .append("th { background-color: #555555; }")
                    .append(".actions { display: flex; flex-direction: column; }")
                    .append(".title { font-size: smaller; color: #aaaaaa; }")
                    .append(".update-btn, .delete-btn, .add-btn, a { border: none; color: #ffffff; padding: 8px 16px; text-align: center; text-decoration: none; display: inline-block; font-size: 14px; margin: 4px 2px; cursor: pointer; border-radius: 20px; }")
                    .append(".update-btn { background-color: #00FF00; }")
                    .append(".delete-btn { background-color: #FF0000; }")
                    .append(".add-btn { background-color: #4682B4; border-radius: 20px; color: #ffffff; }")
                    .append(".update-btn:hover, .delete-btn:hover, .add-btn:hover, a:hover { background-color: #6a0d9b; }")
                    .append("</style>")
                    .append("</head><body>")
                    .append("<h2>Search Results</h2>")
                    .append("<table><tr><th>ID</th><th>Name</th><th>Surname</th><th>Age</th><th>Gender</th><th>Actions</th></tr>");
            while (resultSet.next()) {
                result.append("<tr>")
                        .append("<td>").append(resultSet.getInt("id")).append("</td>")
                        .append("<td>").append(resultSet.getString("name")).append("</td>")
                        .append("<td>").append(resultSet.getString("surname")).append("</td>")
                        .append("<td>").append(resultSet.getInt("age")).append("</td>")
                        .append("<td>").append(resultSet.getString("gender")).append("</td>")
                        .append("<td class=\"actions\">")
                        .append("<form method=\"post\" action=\"/update\">")
                        .append("<input type=\"hidden\" name=\"id\" value=\"").append(resultSet.getInt("id")).append("\">")
                        .append("<input type=\"text\" name=\"name\" value=\"").append(resultSet.getString("name")).append("\">")
                        .append("<input type=\"text\" name=\"surname\" value=\"").append(resultSet.getString("surname")).append("\">")
                        .append("<input type=\"text\" name=\"age\" value=\"").append(resultSet.getInt("age")).append("\">")
                        .append("<input type=\"text\" name=\"gender\" value=\"").append(resultSet.getString("gender")).append("\">")
                        .append("<button class=\"update-btn\" type=\"submit\">Update</button></form>")
                        .append("<form method=\"post\" action=\"/delete/").append(resultSet.getInt("id")).append("\">")
                        .append("<button class=\"delete-btn\" type=\"submit\">Delete</button></form>")
                        .append("</td></tr>");
            }
            result.append("</table>")
                    .append("<br>")
                    .append("<a href=\"/\" style=\"color: #ffffff; border-radius: 20px; padding: 8px 16px; background-color: #FF0000; text-decoration: none;\">Back to Home</a>")
                    .append("</body></html>");
            connection.close();
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage());
        }
        return result.toString();
    }
    private static String getDataFromDatabase() {
        StringBuilder result = new StringBuilder();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/university", "root", "");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from teachers");
            result.append("<html><head>")
                    .append("<style>")
                    .append("body { font-family: Arial, sans-serif; margin: 20px; padding-left: 50px; padding-right: 50px; background-color: #000000; color: #ffffff; border-radius: 20px; }")
                    .append("table { border-collapse: collapse; width: 100%; background-color: #333333; color: #ffffff; border-radius: 20px; }")
                    .append("th, td { border: 1px solid #dddddd; text-align: left; padding: 10px; border-radius: 20px; }")
                    .append("th { background-color: #555555; }")
                    .append(".actions { display: flex; flex-direction: column; }")
                    .append(".title { font-size: smaller; color: #aaaaaa; }")
                    .append(".search-btn, .update-btn, .delete-btn, .add-btn, a { background-color: #800080; border: none; color: #ffffff; padding: 8px 12px; text-align: center; text-decoration: none; display: inline-block; font-size: 12px; margin: 4px 2px; cursor: pointer; border-radius: 20px; }")
                    .append(".add-btn { background-color: #4682B4; }")
                    .append(".update-btn { background-color: #00FF00; }")
                    .append(".delete-btn { background-color: #FF0000; }")
                    .append(".search-btn:hover, .update-btn:hover, .delete-btn:hover, .add-btn:hover, a:hover { background-color: #6a0d9b; }")
                    .append("</style>")
                    .append("</head><body>")
                    .append("<h2>Add New Person</h2>")
                    .append("<form method=\"get\" action=\"/add\">")
                    .append("<button class=\"add-btn\" type=\"submit\">Add New Person</button></form>")
                    .append("<h2><a href=\"http://localhost:8080\">Students</a></h2>")
                    .append("<h2><a href=\"http://localhost:8082\">Employees</a></h2>")
                    .append("<table><tr><th>ID</th><th>Name</th><th>Surname</th><th>Age</th><th>Gender</th><th>Actions</th></tr>");
            while (resultSet.next()) {
                result.append("<tr>")
                        .append("<td>").append(resultSet.getInt("id")).append("</td>")
                        .append("<td>")
                        .append("<form method=\"post\" action=\"/update\">")
                        .append("<input type=\"hidden\" name=\"id\" value=\"").append(resultSet.getInt("id")).append("\">")
                        .append("<input type=\"text\" name=\"name\" value=\"").append(resultSet.getString("name")).append("\">")
                        .append("<div class=\"title\">Name</div>")
                        .append("</td>")
                        .append("<td>")
                        .append("<input type=\"text\" name=\"surname\" value=\"").append(resultSet.getString("surname")).append("\">")
                        .append("<div class=\"title\">Surname</div>")
                        .append("</td>")
                        .append("<td>")
                        .append("<input type=\"text\" name=\"age\" value=\"").append(resultSet.getInt("age")).append("\">")
                        .append("<div class=\"title\">Age</div>")
                        .append("</td>")
                        .append("<td>")
                        .append("<input type=\"text\" name=\"gender\" value=\"").append(resultSet.getString("gender")).append("\">")
                        .append("<div class=\"title\">Gender</div>")
                        .append("</td>")
                        .append("<td class=\"actions\">")
                        .append("<button class=\"update-btn\" type=\"submit\">Update</button></form>")
                        .append("<form method=\"post\" action=\"/delete/").append(resultSet.getInt("id")).append("\">")
                        .append("<button class=\"delete-btn\" type=\"submit\">Delete</button></form>")
                        .append("</td></tr>");
            }
            result.append("</table>")
                    .append("</body></html>");
            connection.close();
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage());
        }
        return result.toString();
    }
    private static void sendResponse(HttpExchange t, String response) throws IOException {
        t.getResponseHeaders().set("Content-Type", "text/html");
        t.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    private static void updateDataInDatabase(String query) {
        try {
            String[] params = query.split("&");
            int id = Integer.parseInt(params[0].split("=")[1]);
            String name = params[1].split("=")[1];
            String surname = params[2].split("=")[1];
            int age = Integer.parseInt(params[3].split("=")[1]);
            String gender = params[4].split("=")[1];
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/university", "root", "");
            String updateQuery = "update teachers set name=?, surname=?, age=?, gender=? where id=?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setInt(5, id);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, surname);
                preparedStatement.setInt(3, age);
                preparedStatement.setString(4, gender);
                preparedStatement.executeUpdate();
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}