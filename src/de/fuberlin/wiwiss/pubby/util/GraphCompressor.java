package de.fuberlin.wiwiss.pubby.util;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.openjena.atlas.lib.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Tool methods to implement approaches for the compact string representation
 * of Graphs.
 *
 * @author Kai Eckert (kai@informatik.uni-mannheim.de)
 * @version $Id$
 */
public class GraphCompressor {



    public String encodeBZip2Base64(String input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64.Base64OutputStream b64 = new Base64.Base64OutputStream(baos);
        BZip2CompressorOutputStream bzip = null;
        try {
            bzip = new BZip2CompressorOutputStream(b64);
            StringReader sr = new StringReader(input);
            int b;
            while ((b = sr.read()) != -1) {
                bzip.write(b);
            }
            bzip.finish();
            bzip.flush();
            b64.flush();
            baos.flush();
            String res = baos.toString("UTF-8");
            bzip.close();
            b64.close();
            baos.close();
            return res;
        } catch (IOException e) {
            try {
                if (bzip!=null) bzip.close();
                if (b64!=null) b64.close();
                if (baos!=null) baos.close();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            throw new RuntimeException(e);
        }

    }

    public String decodeBase64BZip2(String input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64.Base64OutputStream b64 = new Base64.Base64OutputStream(baos);
        BZip2CompressorOutputStream bzip = null;
        try {
            bzip = new BZip2CompressorOutputStream(b64);
            StringReader sr = new StringReader(input);
            int b;
            while ((b = sr.read()) != -1) {
                bzip.write(b);
            }
            bzip.finish();
            bzip.flush();
            b64.flush();
            baos.flush();
            String res = baos.toString("UTF-8");
            bzip.close();
            b64.close();
            baos.close();
            return res;
        } catch (IOException e) {
            try {
                if (bzip!=null) bzip.close();
                if (b64!=null) b64.close();
                if (baos!=null) baos.close();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            throw new RuntimeException(e);
        }

    }
}
