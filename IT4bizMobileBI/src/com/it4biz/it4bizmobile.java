package com.it4biz;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class it4bizmobile extends Activity {

  static Document repositoryDocument;
  static String selectedRepositoryPath = "/";

  private static String getSolution(String fullPath) {
    // solution is between first and second slashes
    int firstSlashIndex = fullPath.indexOf("/");
    int secondSlashIndex = fullPath.indexOf("/", firstSlashIndex + 1);
    return fullPath.substring(firstSlashIndex, secondSlashIndex);
  }

  private static String getSolutionPath(String fullPath) {
    int firstSlashIndex = fullPath.indexOf("/");
    int secondSlashIndex = fullPath.indexOf("/", firstSlashIndex + 1);
    return fullPath.substring(secondSlashIndex);
  }

  public void initRepoView() {
    final ListView listView = (ListView) findViewById(R.id.navigation);
    try {

      final List<String> repoLocalizedContent = new ArrayList<String>();
      final List<Element> repoElementContent = new ArrayList<Element>();

      listView.setOnItemClickListener(new OnItemClickListener() {
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
          boolean isDir = "true".equals(repoElementContent.get(arg2).getAttribute("isDirectory"));
          if (isDir) {
            selectedRepositoryPath += repoElementContent.get(arg2).getAttribute("name") + "/";
            Toast.makeText(it4bizmobile.this, selectedRepositoryPath, Toast.LENGTH_SHORT).show();
            initRepoView();
          } else {
            // open in webview
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            // it4biz
            String serverurl = prefs.getString("serverurl", "http://bimobile.it4biz.com.br/pentaho");

            String url = repoElementContent.get(arg2).getAttribute("url");
            if (url == null || "".equals(url)) {
              String solution = getSolution(selectedRepositoryPath);
              String path = getSolutionPath(selectedRepositoryPath);
              String action = repoElementContent.get(arg2).getAttribute("name");
              url = serverurl + "/ViewAction?solution=" + solution + "&path=" + path + "&action=" + action;
            }
            if (!url.startsWith("http")) {
              url = serverurl + "/" + url;
            }
            Toast.makeText(it4bizmobile.this, url, Toast.LENGTH_SHORT).show();

            // it4biz
            String userid = prefs.getString("username", "demo");
            String password = prefs.getString("password", "demo");
            if (url.indexOf("?") == -1) {
              url += "?";
            }
            url += "&userid=" + userid + "&password=" + password;

            // WebView webView = new WebView(HelloActivity.this);
            // webView.getSettings().setBuiltInZoomControls(true);
            // webView.loadUrl(url);
            // setContentView(webView);
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(myIntent);
            return;
          }
        }
      });

      // find selectedRepositoryFilePath
      NodeList children = findElement(repositoryDocument.getDocumentElement(), selectedRepositoryPath).getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Element element = (Element) children.item(i);
        boolean visible = "true".equals(element.getAttribute("visible"));
        if (visible) {
          repoLocalizedContent.add(element.getAttribute("localized-name"));
          repoElementContent.add(element);
        }
      }

      ArrayAdapter<String> listadapter = new ArrayAdapter<String>(it4bizmobile.this, android.R.layout.simple_list_item_1, repoLocalizedContent) {
        public View getView(int position, View convertView, ViewGroup parent) {
          TextView txt = new TextView(this.getContext());

          boolean isDir = "true".equals(repoElementContent.get(position).getAttribute("isDirectory"));
          if (isDir) {
        	// it4biz
            txt.setTextColor(Color.BLUE);
          } else {
            if (repoElementContent.get(position).getAttribute("url") == null || "".equals(repoElementContent.get(position).getAttribute("url"))) {
              //it4biz
              txt.setTextColor(Color.YELLOW);
            } else {
              //it4biz
              txt.setTextColor(Color.RED);
            }
          }
          txt.setGravity(Gravity.FILL_HORIZONTAL);
          txt.setSingleLine();
          txt.setTextSize(22);
          txt.setText(this.getItem(position));
          return txt;
        }

      };
      listView.setAdapter(listadapter);
      // listView.setAdapter(new ArrayAdapter<String>(HelloActivity.this, android.R.layout.simple_list_item_1, repoLocalizedContent));
    } catch (Throwable t) {
      Toast.makeText(it4bizmobile.this, t.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  public Element findElement(Element parentElement, String path) {
    Element found = parentElement;

    // get the first part of the path
    StringTokenizer st = new StringTokenizer(path, "/");
    if (st.hasMoreTokens()) {
      String token = st.nextToken();
      NodeList children = parentElement.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Element child = (Element) children.item(i);
        String name = child.getAttribute("name");
        if (token.equals(name)) {
          // chop off
          String newPath = path.substring(path.indexOf(name) + name.length());
          return findElement(child, newPath);
        }
      }
    }

    return found;
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {

      if (contentView == R.layout.login) {
        finish();
      }

      if (selectedRepositoryPath == null || "/".equals(selectedRepositoryPath) || selectedRepositoryPath.lastIndexOf("/") < 0) {
        repositoryDocument = null;
        selectedRepositoryPath = "/";
        setContentView(R.layout.login);
        return true;
      }

      // back the path off and reload navigate, if we have some path
      if (selectedRepositoryPath != null && selectedRepositoryPath.endsWith("/")) {
        selectedRepositoryPath = selectedRepositoryPath.substring(0, selectedRepositoryPath.length() - 2);
      }
      if (selectedRepositoryPath != null && selectedRepositoryPath.lastIndexOf("/") >= 0) {
        selectedRepositoryPath = selectedRepositoryPath.substring(0, selectedRepositoryPath.lastIndexOf("/"));
        if (!selectedRepositoryPath.startsWith("/")) {
          selectedRepositoryPath = "/" + selectedRepositoryPath;
        }
        if (!selectedRepositoryPath.endsWith("/")) {
          selectedRepositoryPath += "/";
        }
        Toast.makeText(it4bizmobile.this, selectedRepositoryPath, Toast.LENGTH_SHORT).show();
        setContentView(R.layout.navigate);
      }
      return true;
    } else {
      return super.onKeyDown(keyCode, event);
    }
  }

  protected void onPause() {
    super.onPause();
  }

  protected void onSaveInstanceState(Bundle outState) {

    if (outState != null) {
      outState.putInt("contentView", contentView);
    }

    super.onSaveInstanceState(outState);

  }

  private int contentView = R.layout.login;

  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    contentView = layoutResID;

    if (contentView == R.layout.navigate) {
      initRepoView();
    } else if (contentView == R.layout.login) {
      Button cancelButton = (Button) findViewById(R.id.cancel);
      cancelButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          // it4biz
          Toast.makeText(it4bizmobile.this, "Saindo...", Toast.LENGTH_SHORT);
          finish();
        }
      });

      final EditText password = (EditText) findViewById(R.id.password);
      final EditText username = (EditText) findViewById(R.id.username);
      final EditText serverEditText = (EditText) findViewById(R.id.serverurl);
      final CheckBox remember = (CheckBox) findViewById(R.id.remember);

      SharedPreferences prefs = getPreferences(MODE_PRIVATE);

      // it4biz
      username.setText(prefs.getString("username", "demo"));
      password.setText(prefs.getString("password", "demo"));
      serverEditText.setText(prefs.getString("serverurl", "http://bimobile.it4biz.com.br/pentaho"));

      Button button = (Button) findViewById(R.id.ok);
      button.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {

          if (remember.isChecked()) {
            Editor editor = getPreferences(MODE_PRIVATE).edit();
            editor.putString("username", username.getText().toString());
            editor.putString("password", password.getText().toString());
            editor.putString("serverurl", serverEditText.getText().toString());
            editor.commit();
          }

          AsyncTask<Object, Integer, Document> task = new AsyncTask<Object, Integer, Document>() {

            ProgressDialog myProgressDialog;
            InputStream is;

            protected void onPreExecute() {
              myProgressDialog = ProgressDialog.show(it4bizmobile.this, "Por favor espere ...", "Entrando no Servidor IT4biz BI...", true, true);
              try {
                URL url = new URL(serverEditText.getText().toString() + "/SolutionRepositoryService?component=getSolutionRepositoryDoc&userid="
                    + username.getText().toString() + "&password=" + password.getText().toString());
                Log.d("blah", "here1");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.getHeaderFields();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                  Toast.makeText(it4bizmobile.this, "Usuário ou senha inválido", Toast.LENGTH_SHORT).show();
                  myProgressDialog.dismiss();
                  cancel(true);
                } else if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                  is = conn.getInputStream();
                } else {
                  Toast.makeText(it4bizmobile.this, "Não foi possível conectar, código do erro: " + conn.getResponseCode(), Toast.LENGTH_SHORT).show();
                  myProgressDialog.dismiss();
                  cancel(true);
                }
              } catch (Throwable t) {
                Toast.makeText(it4bizmobile.this, "Não foi possível contatar o servidor", Toast.LENGTH_SHORT).show();
                myProgressDialog.dismiss();
                cancel(true);
              }
            }

            protected void onPostExecute(Document doc) {
              if (doc == null) {
                Toast.makeText(it4bizmobile.this, "Informações do repositório não podem ser trazidas", Toast.LENGTH_SHORT).show();
                myProgressDialog.dismiss();
                cancel(true);
              } else {
                myProgressDialog.dismiss();
                setContentView(R.layout.navigate);
              }
            }

            protected Document doInBackground(Object... object) {
              try {
                repositoryDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                return repositoryDocument;
              } catch (Throwable t) {
              }
              return null;
            }
          };
          task.execute();

        }
      });
    }

  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      contentView = savedInstanceState.getInt("contentView", contentView);
    }
    setContentView(contentView);

  }
}