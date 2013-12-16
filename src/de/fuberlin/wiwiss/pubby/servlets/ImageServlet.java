package de.fuberlin.wiwiss.pubby.servlets;

import org.imgscalr.Scalr;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;

/**
 * An image cache and thumbnail generator
 *
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public class ImageServlet extends HttpServlet {


    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getParameter("url");
        int width = Integer.parseInt(req.getParameter("width"));
        int height = Integer.parseInt(req.getParameter("height"));
        url = URLDecoder.decode(url,"utf-8");
        BufferedImage buffer = null;
        ImageIO.setCacheDirectory(new File("/tmp"));
        ImageIO.setUseCache(true);

        try {

            buffer = ImageIO.read(new URL(url));
        } catch (MalformedURLException e) {

        }

        if (buffer != null) {
            buffer = Scalr.resize(buffer, Scalr.Mode.FIT_TO_WIDTH,width,height,null);
            String mime = "image/jpeg";
            OutputStream os = resp.getOutputStream();

            resp.setContentType(mime);
            Iterator i = ImageIO.getImageWritersByMIMEType("image/jpeg");
            ImageWriter iw = (ImageWriter) i.next();
            IIOImage iioimage = new IIOImage(buffer, null, null);
            ImageWriteParam p = iw.getDefaultWriteParam();

            iw.setOutput(ImageIO.createImageOutputStream(os));
            iw.write(null, iioimage, p);

            os.flush();
            os.close();


        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, " NOT FOUND! "+url );
        }




    }
}
