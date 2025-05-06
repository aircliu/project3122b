package fablix;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private static final String URL  = "jdbc:mysql://localhost:3306/moviedb";
    private static final String USER = "mytestuser";
    private static final String PASS = "My6$Password";
    
    // Your reCAPTCHA Secret Key - replace with your actual key
    private static final String RECAPTCHA_SECRET = "6Le_sC8rAAAAAMG06nORm0zI9XX5lvRvG47aiUlm";
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        JsonObject outJson = new JsonObject();
        
        // Get reCAPTCHA response from the form
        String recaptchaResponse = req.getParameter("g-recaptcha-response");
        
        // Verify reCAPTCHA
        boolean recaptchaVerified = verifyRecaptcha(recaptchaResponse);
        
        if (!recaptchaVerified) {
            outJson.addProperty("success", false);
            outJson.addProperty("message", "Please complete the reCAPTCHA verification.");
            out.write(outJson.toString());
            return;
        }
        
        // If reCAPTCHA is verified, proceed with login
        String email = req.getParameter("email");
        String pw    = req.getParameter("password");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection c = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT firstName, password FROM customers WHERE email=?")) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getString("password").equals(pw)) { 
                        HttpSession s = req.getSession(true);
                        s.setAttribute("user", rs.getString("firstName"));
                        s.setAttribute("email", email); // Store email for later use
                        outJson.addProperty("success", true);
                    }
                    else {  // 
                        outJson.addProperty("success", false);
                        outJson.addProperty("message", "Invalid password.");
                    }
                } else {                                 
                    outJson.addProperty("success", false);
                    outJson.addProperty("message", "Invalid email.");
                }
            }
            out.write(outJson.toString());
        } catch (Exception e) {
            outJson.addProperty("success", false);
            outJson.addProperty("message", "Server error: " + e.getMessage());
            out.write(outJson.toString());
            out.close();
        }
    }
    
    /**
     * Verifies the reCAPTCHA response with Google's API
     */
    private boolean verifyRecaptcha(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            return false;
        }
        
        try {
            URL url = new URL(RECAPTCHA_VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            
            // Prepare the request data
            String postData = "secret=" + RECAPTCHA_SECRET + 
                             "&response=" + recaptchaResponse;
            
            // Send the request
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(postData);
            writer.flush();
            
            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse the JSON response
            String jsonResponse = response.toString();
            // Simple check for "success":true in the response
            return jsonResponse.contains("\"success\":true");
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}