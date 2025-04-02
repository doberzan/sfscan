package org.xfr.ui;

import org.xfr.SFScan;

import javax.swing.*;

public class UserInterface extends JTabbedPane {
    private SFScan sfScan;
    public MethodsTab methodsTab;
    public ObjectsTab objectsTab;
    public RecordsTab recordsTab;
    public UserInterface(SFScan sfScan) {
        this.sfScan = sfScan;
        this.methodsTab = new MethodsTab(sfScan);
        this.objectsTab = new ObjectsTab(sfScan);
        this.recordsTab = new RecordsTab(sfScan);

        this.add("Methods",methodsTab);
        this.add("Objects",objectsTab);
        this.add("Records",recordsTab);
    }

}