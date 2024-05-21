package Controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import model.ProductModel;
import model.game;

/**
 * Servlet implementation class AddGame
 */
@WebServlet("/AddGame")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1 MB
    maxFileSize = 1024 * 1024 * 5,   // 5 MB
    maxRequestSize = 1024 * 1024 * 10 // 10 MB
)
public class UploadGame extends HttpServlet {
    private static final long serialVersionUID = 1L;
    static String SAVE_DIR = "img";
    static ProductModel GameModels = new ProductModelDM();
    
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDateTime now = LocalDateTime.now();
    
    public UploadGame() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/plain");
        out.write("Error: GET method is used but POST method is required");
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Collection<?> games = (Collection<?>) request.getSession().getAttribute("games");
        String savePath = request.getServletContext().getRealPath("") + File.separator + SAVE_DIR;
        game g1 = new game();
        
        String fileName = null;
        String message = "upload =\n";
        if (request.getParts() != null && request.getParts().size() > 0) {
            for (Part part : request.getParts()) {
                fileName = extractFileName(part);
                fileName = sanitizeFileName(fileName);
                
                if (fileName != null && !fileName.equals("")) {
                    // Check if the file is an image using magic number and file extension
                    if (isValidImage(part, fileName)) {
                        part.write(savePath + File.separator + fileName);
                        g1.setImg(fileName);
                        message = message + fileName + "\n";
                    } else {
                        request.setAttribute("error", "Errore: Il file selezionato non è un'immagine valida (solo JPG e PNG sono accettati)");
                    }
                } else {
                    request.setAttribute("error", "Errore: Bisogna selezionare almeno un file");
                }
            }
        }
        
        g1.setName(sanitizeInput(request.getParameter("nomeGame")));
        g1.setYears(sanitizeInput(request.getParameter("years")));
        g1.setAdded(dtf.format(now));
        g1.setQuantity(Integer.valueOf(request.getParameter("quantita")));
        g1.setPEG(Integer.valueOf(request.getParameter("PEG")));
        g1.setIva(Integer.valueOf(request.getParameter("iva")));
        g1.setGenere(sanitizeInput(request.getParameter("genere")));
        g1.setDesc(sanitizeInput(request.getParameter("desc")));
        g1.setPrice(Float.valueOf(request.getParameter("price")));
        
        try {
        	
            GameModels.doSave(g1);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Errore durante il salvataggio del gioco. Riprova più tardi.");
        }
        request.setAttribute("stato", "success!");
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/gameList?page=admin&sort=added DESC");
        dispatcher.forward(request, response);
    }

    
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }
    
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }
    private boolean isValidImage(Part part, String fileName) throws IOException {
        // Controllo dell'estensione del file
        String lowerCaseFileName = fileName.toLowerCase();
        if (!(lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg") || lowerCaseFileName.endsWith(".png"))) {
            return false;
        }

        // Controllo del magic number
        try (InputStream inputStream = part.getInputStream()) {
            byte[] buffer = new byte[8];
            if (inputStream.read(buffer) != -1) {
                String magicNumber = bytesToHex(buffer).toUpperCase();
                return magicNumber.startsWith("FFD8FF") || // JPEG
                       magicNumber.startsWith("89504E470D0A1A0A"); // PNG
            }
        }
        return false;
    }


    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[<>\"']", "");
    }
}
