package it.bestapp.paganino.utility.thread;


import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;


import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import it.bestapp.paganino.adapter.bustapaga.BustaPaga;
import it.bestapp.paganino.utility.SingletonParametersBridge;
import it.bestapp.paganino.utility.connessione.HRConnect;
import it.bestapp.paganino.utility.connessione.PageDownloadedInterface;
import it.bestapp.paganino.utility.db.DataBaseAdapter;
import it.bestapp.paganino.utility.parser.BustaPagaParser;
import it.bestapp.paganino.utility.setting.SettingsManager;



public class ThreadPDF extends AsyncTask<Void, Void, File> {

    private Fragment frag = null;
    private HRConnect con = null;
    private BustaPaga bPaga = null;
    private PageDownloadedInterface pCall = null;
    private SingletonParametersBridge singleton;
    private String path;
    private char mode;

    public ThreadPDF(Fragment f, HRConnect c, BustaPaga bP, char m) {
        this.frag = f;
        this.mode = m;
        this.con = c;
        this.bPaga = bP;
        this.pCall = (PageDownloadedInterface) f;


        singleton = SingletonParametersBridge.getInstance();
        SettingsManager settings = (SettingsManager) singleton.getParameter("settings");
        path = settings.getPath();
        File file = new File(path);
        if (!file.exists())
            file.mkdirs();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pCall.procesDialog(true);
    }

    @Override
    public File doInBackground(Void... params) {
        int count;
        InputStream in;
        FileOutputStream out = null;

        File  f = new File(path , bPaga.getID() + ".pdf");
        if (!f.exists()) {
            try {
                in = con.getPDF( bPaga.getID() );
                out = new FileOutputStream(f);
                byte data[] = new byte[1024];
                while ((count = in.read(data)) != -1)
                    out.write(data, 0, count);
                out.flush();
                out.close();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    @Override
    protected void onPostExecute(File f) {
        pCall.procesDialog(false);
        pCall.onPDFDownloaded(bPaga, f, mode);
    }
}