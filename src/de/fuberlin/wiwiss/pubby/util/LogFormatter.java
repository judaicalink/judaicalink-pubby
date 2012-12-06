package de.fuberlin.wiwiss.pubby.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    public String format(LogRecord logRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(logRecord.getMillis())));
        sb.append(" ");
        sb.append(logRecord.getLevel().getName());
        sb.append(" ");
        sb.append(logRecord.getSourceClassName());
        sb.append(": ");
        sb.append(logRecord.getMessage());
        sb.append("\n");
        return sb.toString();
    }
}
