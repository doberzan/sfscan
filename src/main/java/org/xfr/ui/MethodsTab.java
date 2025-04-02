package org.xfr.ui;

import org.xfr.SFScan;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

public class MethodsTab extends JPanel implements ActionListener, ListSelectionListener {
    private final SFScan sfScan;

    private JButton updateButton;
    private JButton outputButton;
    private JTextArea requestTextArea;

    public JList<String> methods;
    public JList<String> parameters;
    public DefaultListModel<String> methodsModel;
    public DefaultListModel<String> parametersModel;
    public HashMap<String, String> exampleRequestForEndpoints;
    public HashMap<String, TreeSet<String>> endPoints;

    public MethodsTab(SFScan sfScan) {
        this.sfScan = sfScan;
        this.setLayout(new BorderLayout());

        // Initialize objects
        endPoints = new HashMap<>();
        exampleRequestForEndpoints = new HashMap<>();
        methodsModel = new DefaultListModel<>();
        methods = new JList<>(methodsModel);
        parametersModel = new DefaultListModel<>();
        parameters = new JList<>(parametersModel);

        buildPanelUI();
    }

    private void buildPanelUI() {

        Font font = new Font("SansSerif", Font.BOLD, 16);

        // Build labels
        JLabel endpointsLabel = new JLabel(" API Endpoints");
        JLabel parametersLabel = new JLabel(" Parameters");
        JLabel requestLabel = new JLabel(" Request");
        endpointsLabel.setFont(font);
        parametersLabel.setFont(font);
        requestLabel.setFont(font);

        // Build request text area
        requestTextArea = new JTextArea();
        requestTextArea.setEditable(false);
        requestTextArea.setLineWrap(true);
        requestTextArea.setWrapStyleWord(true);

        // Build buttons
        updateButton = new JButton("Update");
        outputButton = new JButton("Save API Data");

        // Build scroll panes
        JScrollPane methodsScrollPane = new JScrollPane(methods);
        JScrollPane parametersScrollPane = new JScrollPane(parameters);
        JScrollPane requestAreaScrollPane = new JScrollPane(requestTextArea);

        // Build methods collection
        JPanel methodsCollection = new JPanel();
        methodsCollection.setLayout(new BorderLayout());
        methodsCollection.add(endpointsLabel, BorderLayout.NORTH);
        methodsCollection.add(methodsScrollPane, BorderLayout.CENTER);

        // Build parameters collection
        JPanel parametersCollection = new JPanel();
        parametersCollection.setLayout(new BorderLayout());
        parametersCollection.add(parametersLabel, BorderLayout.NORTH);
        parametersCollection.add(parametersScrollPane, BorderLayout.CENTER);

        // Build requests collection
        JPanel requestCollection = new JPanel();
        requestCollection.setLayout(new BorderLayout());
        requestCollection.add(requestLabel, BorderLayout.NORTH);
        requestCollection.add(requestAreaScrollPane, BorderLayout.CENTER);

        // Build button collection
        JPanel buttonCollection = new JPanel();
        buttonCollection.setLayout(new GridLayout(1, 2));
        buttonCollection.add(outputButton);
        buttonCollection.add(updateButton);

        // Build split pane containing parameters and methods
        JSplitPane leftPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftPanel.setRightComponent(parametersCollection);
        leftPanel.setLeftComponent(methodsCollection);

        // Build split pane containing requests panel and left-panel (parameters+methods)
        JSplitPane rightPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightPanel.setRightComponent(requestCollection);
        rightPanel.setLeftComponent(leftPanel);

        // Add listeners
        updateButton.addActionListener(this);
        outputButton.addActionListener(this);
        methods.addListSelectionListener(this);

        // Add items to main panel and set cosmetic preferences
        rightPanel.setBorder(null);
        leftPanel.setBorder(null);
        methodsCollection.setBorder(null);
        parametersCollection.setBorder(null);
        requestCollection.setBorder(null);
        requestTextArea.setBorder(null);
        updateButton.setPreferredSize(new Dimension(100, 50));
        methodsScrollPane.setBorder(null);
        parametersScrollPane.setBorder(null);
        requestAreaScrollPane.setBorder(null);
        this.add(rightPanel, BorderLayout.CENTER);
        this.add(buttonCollection, BorderLayout.SOUTH);
        leftPanel.setDividerLocation(450);
        rightPanel.setDividerLocation(1000);

    }

    public void updateMethods(){
        methodsModel.clear();
        parametersModel.clear();

        // Sort list keys
        String[] tmp = endPoints.keySet().toArray(new String[0]);
        if (tmp.length > 0) {
            Arrays.sort(tmp);
        }

        // Add sorted elements to methods pane
        for (String s : tmp)
        {
            methodsModel.addElement(s +"\n");
        }
    }

    private void updateParameters(){
        parametersModel.clear();
        if(methods.getSelectedValue() != null) {
            for (String param : endPoints.get(methods.getSelectedValue().strip())) {
                if(param != null) {
                    parametersModel.addElement(param);
                }
            }
        }
    }

    private void printAPIList()
    {
        String[] tmp = endPoints.keySet().toArray(new String[0]);
        Arrays.sort(tmp);
        sfScan.log.logToOutput("======API Listing======\n");
        for (String s : tmp)
        {
            sfScan.log.logToOutput(s);
        }
    }

    private void updateRequestTextArea() {
        if(methods.getSelectedValue() != null) {
            requestTextArea.setText(exampleRequestForEndpoints.get(methods.getSelectedValue().strip()));
        }
        requestTextArea.setCaretPosition(0);
    }

    private void saveData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("api-list.txt"));

        if (fileChooser.showSaveDialog(this.getParent()) == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile().getAbsolutePath()))) {
                String[] tmp = endPoints.keySet().toArray(new String[0]);
                Arrays.sort(tmp);
                for (String s : tmp)
                {
                    writer.write(s +"\n");
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == updateButton) {
            updateMethods();
        }
        if (e.getSource() == outputButton) {
            saveData();
        }

        printAPIList();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

        // New list item is selected on the methods tab
        if(e.getSource() == methods)
        {
            updateRequestTextArea();
            updateParameters();
        }
    }
}
