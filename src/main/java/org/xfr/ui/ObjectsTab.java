package org.xfr.ui;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xfr.SFScan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class ObjectsTab extends JPanel implements ActionListener {
    private final SFScan sfScan;

    private JButton queryButton;
    private JButton saveButton;

    public JTextField targetURL;
    private JTable objectsTable;

    private DefaultTableModel tableModel;

    public HashMap<String, String> sfObjects;

    public ObjectsTab(SFScan sfScan) {
        this.sfScan = sfScan;
        this.sfObjects = new HashMap<>();
        buildPanelUI();
    }

    private void buildPanelUI() {
        this.setLayout(new BorderLayout());

        queryButton = new JButton("Query");
        saveButton = new JButton("Save Objects");
        targetURL = new JTextField("<target domain>");

        JPanel buttonCollection = new JPanel();
        buttonCollection.setLayout(new GridLayout(1, 3));

        saveButton.setPreferredSize(new Dimension(100, 50));

        // Build panel
        objectsTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(objectsTable);

        // Add listeners
        queryButton.addActionListener(this);
        saveButton.addActionListener(this);

        // Add items to panel
        buttonCollection.add(saveButton);
        buttonCollection.add(queryButton);
        buttonCollection.add(targetURL);
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(buttonCollection, BorderLayout.SOUTH);
    }

    public void sendObjectsQuery() {
        String payload = "{\"actions\":[{\"id\":\"1337;a\",\"descriptor\":\"serviceComponent://ui.force.components.controllers.hostConfig.HostConfigController/ACTION$getConfigData\",\"callingDescriptor\":\"UNKNOWN\",\"params\":{},\"storable\":true}]}";
        String fullQuery = "message=" + URLEncoder.encode(payload, StandardCharsets.UTF_8) + "&aura.context=" + sfScan.httpTap.currentContext + "&aura.token=null";

        if (targetURL.getText() != null && !targetURL.getText().isEmpty()) {

            // New requests require a new thread
            try {
                Thread thread = new Thread(() -> {
                    HttpRequest req = HttpRequest.httpRequestFromUrl("https://"+targetURL.getText()+"/s/sfsites/aura");
                    req = req.withMethod("POST");
                    req = req.withHeader("Content-Type", "application/x-www-form-urlencoded");
                    req = req.withHeader("Host", targetURL.getText());
                    req = req.withBody(fullQuery);
                    HttpRequestResponse res = sfScan.api.http().sendRequest(req);
                    ObjectMapper om = new ObjectMapper();
                    JsonNode baseObject;
                    try {
                        baseObject = om.readTree(res.response().bodyToString());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    parseSFObjects(baseObject);

                    String[] keys = sfObjects.keySet().toArray(new String[0]);
                    Arrays.sort(keys);
                    String[][] data = new String[keys.length][];
                    int ctr = 0;
                    for (String key : keys)
                    {
                        data[ctr] = new String[]{key,sfObjects.get(key)};
                        ctr ++;
                    }
                    String[] columnNames = {"Object Names ("+keys.length+")", "ID Prefix "};
                    tableModel = new DefaultTableModel(data, columnNames);
                    objectsTable.setModel(tableModel);
                });
                thread.start();
            } catch (NullPointerException e) {
                sfScan.log.logToError(Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private void parseSFObjects(JsonNode baseObject) {
        Iterator<String> i = baseObject.get("actions").get(0).get("returnValue").get("apiNamesToKeyPrefixes").fieldNames();
        String key;

        // I dont trust iterators
        int failSafe = 10000;
        while (i.hasNext() && failSafe > 0)
        {
            failSafe -= 1;
            key = i.next();
            sfObjects.put(key, baseObject.get("actions").get(0).get("returnValue").get("apiNamesToKeyPrefixes").get(key).textValue());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == queryButton) {
            sendObjectsQuery();
        }
        if (e.getSource() == saveButton) {
            saveData();
        }
    }

    private void saveData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("objects-list.txt"));

        if (fileChooser.showSaveDialog(this.getParent()) == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile().getAbsolutePath()))) {
                String[] tmp = sfObjects.keySet().toArray(new String[0]);
                Arrays.sort(tmp);
                for (String s : tmp)
                {
                    writer.write(s + "," + sfObjects.get(s) + "\n");
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
