import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@WebServlet(name = "Albums", value = "/Albums")
public class Albums extends HttpServlet {

    private final String badConnectionError = "{\"error\":\"Bad Request\",\"message\":\"invalid request\"}";
    private HashMap<Integer, AlbumInfo> albumData = new HashMap<Integer, AlbumInfo>() {{
        put(1, new AlbumInfo("Taylor Swift", "1989 ", "2014"));
        put(2, new AlbumInfo("Joni Mitchell", "Blue", "1971"));
        put(3, new AlbumInfo("Bob Dylan", "Nashville Skyline", "1969"));
        put(4, new AlbumInfo("Black Sabbath", "Master of Reality", "1971"));
        put(5, new AlbumInfo("Pink Floyd", "Wish You Were Here", "1975"));
        put(6, new AlbumInfo("Kanye West", "My Beautiful Dark Twisted Fantasy", "2010"));
        put(7, new AlbumInfo("Fleetwood Mac", "Tango in the Night", "1987"));
        put(8, new AlbumInfo("Chance The Rapper", "Acid Rap", "2013"));
        put(9, new AlbumInfo("Dire Straits", "Brothers in Arms", "1985"));
        put(10, new AlbumInfo("Jim Croce", "You Don’t Mess Around With Jim", "1972"));
    }};

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();
        Gson gson = new Gson();

        // Check if Path is valid or Not
        if(urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(badConnectionError);
            return;
        }

        // Splits URL
        String[] urlPaths = urlPath.split("/");

        if(!isUrlValid(urlPaths)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(badConnectionError);
        } else {
            // Wants Album with ID
            if(albumData.containsKey(Integer.parseInt(urlPaths[1]))) {
                response.getWriter().write(gson.toJson(albumData.get(Integer.parseInt(urlPaths[1]))));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                String wrongAlbumID = "{\"error\":\"Key not found\",\"message\":\"invalid Album ID\"}";
                response.getWriter().write(wrongAlbumID);
            }

            // In-case all  Albums are needed when, /albums are called.
//            response.setStatus(HttpServletResponse.SC_OK);
//            // Get all Albums
//            if(urlPaths.length == 0) {
//                response.getWriter().write(gson.toJson(albumData.values()));
//            } else {
//                // Wants Album with ID
//                if(albumData.containsKey(Integer.parseInt(urlPaths[1]))) {
//                    response.getWriter().write(gson.toJson(albumData.get(Integer.parseInt(urlPaths[1]))));
//                } else {
//                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//                    response.getWriter().write(wrongAlbumID);
//                }
//            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            List<FileItem> items = upload.parseRequest(request);

            byte[] imageBytes = new byte[0];
            String albumJson = null;

            for(FileItem item : items) {
                if(item.isFormField()) {
                    if("profile".equals(item.getFieldName())) {
                        albumJson = item.getString();
                    }
                } else {
                    imageBytes = item.get();
                }
            }

            // Check if Profile is passed
//            if (albumJson == null || albumJson.isEmpty()) {
//                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                response.getWriter().write(badConnectionError);
//                return;
//            }

            AlbumInfo albumInfo = gson.fromJson(albumJson, AlbumInfo.class);
            int newAlbumID = albumData.size() + 1;
            albumData.put(newAlbumID, albumInfo);

            response.setStatus(HttpServletResponse.SC_OK);
            PostResponse postResponse = new PostResponse(Integer.toString(newAlbumID), Integer.toString(imageBytes.length));
            response.getWriter().write(gson.toJson(postResponse));
        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isUrlValid(String[] urlPath) {
        return urlPath.length >= 1;
    }
}