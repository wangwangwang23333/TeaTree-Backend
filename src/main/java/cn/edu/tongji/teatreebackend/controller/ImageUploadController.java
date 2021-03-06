package cn.edu.tongji.teatreebackend.controller;

import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TODO:此处写ImageUploadController类的描述
 *
 * @author 汪明杰
 * @date 2022/3/28 15:06
 */
@RestController
@RequestMapping("/api/image")
public class ImageUploadController {
    private static final long serialVersionUID = 1L;

    @RequestMapping(value = "/files/{fileName}", method = RequestMethod.GET)
    protected void getImage(@PathVariable String fileName, HttpServletResponse response)
            throws ServletException, IOException
    {
        System.out.println(fileName);
        File file = new File("./files/images", fileName);
//        response.setHeader("Content-Type", getServletContext().getMimeType(file));
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
        Files.copy(file.toPath(), response.getOutputStream());
    }


    @RequestMapping(value="/upload", method = RequestMethod.POST)
    protected void uploadImage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        File uploads = new File("./files/images/");
        String multipartContentType = "multipart/form-data";
        String fieldname = "file";
        Part filePart = request.getPart(fieldname);
        Map<Object, Object> responseData = null;

// Create path components to save the file.
        final PrintWriter writer = response.getWriter();

        String linkName = null;
        String name = null;

        try {
            // Check content type.
            if (request.getContentType() == null ||
                    request.getContentType().toLowerCase().indexOf(multipartContentType) == -1) {

                throw new Exception("Invalid contentType. It must be " + multipartContentType);
            }

            // Get file Part based on field name and also image extension.
            filePart = request.getPart(fieldname);
            String type = filePart.getContentType();
            type = type.substring(type.lastIndexOf("/") + 1);

            // Generate random name.
            String extension = type;
            extension = (extension != null && extension != "") ? "." + extension : extension;
            name = UUID.randomUUID().toString() + extension ;

            // Get absolute server path.
            String absoluteServerPath = uploads + name;

            // Create link.
            String path = request.getHeader("referer");
            // 指定返回文件路径
            linkName = "http://localhost:8100/api/image/files/" + name;

            // Validate image.
            String mimeType = filePart.getContentType();
            String[] allowedExts = new String[] {
                    "gif",
                    "jpeg",
                    "jpg",
                    "png",
                    "svg",
                    "blob"
            };
            String[] allowedMimeTypes = new String[] {
                    "image/gif",
                    "image/jpeg",
                    "image/pjpeg",
                    "image/x-png",
                    "image/png",
                    "image/svg+xml"
            };

            if (!ArrayUtils.contains(allowedExts, FilenameUtils.getExtension(absoluteServerPath)) ||
                    !ArrayUtils.contains(allowedMimeTypes, mimeType.toLowerCase())) {

                // Delete the uploaded image if it dose not meet the validation.
                File file = new File(uploads + name);
                if (file.exists()) {
                    file.delete();
                }

                throw new Exception("Image does not meet the validation.");
            }

            // Save the file on server.

            File file = new File(uploads, name);

            try (InputStream input = filePart.getInputStream()) {
                Files.copy(input, file.toPath());
            } catch (Exception e) {
                writer.println("<br/> ERROR: " + e);
            }

        } catch (Exception e) {
            e.printStackTrace();
            writer.println("You either did not specify a file to upload or are " +
                    "trying to upload a file to a protected or nonexistent " +
                    "location.");
            writer.println("<br/> ERROR: " + e.getMessage());
            responseData = new HashMap< Object, Object >();
            responseData.put("error", e.toString());

        } finally {
            // Build response data.
            responseData = new HashMap < Object, Object > ();
            responseData.put("link", linkName);

            // Send response data.
            String jsonResponseData = new Gson().toJson(responseData);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonResponseData);
        }
    }
}
