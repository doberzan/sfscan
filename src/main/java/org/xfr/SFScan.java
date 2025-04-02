package org.xfr;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import org.xfr.listeners.HttpTap;
import burp.api.montoya.logging.Logging;
import org.xfr.ui.UserInterface;

public class SFScan implements BurpExtension
{
    public Logging log;
    public UserInterface ui;
    public HttpTap httpTap;
    public MontoyaApi api;
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        // Enumerate SF custom objects
        // Enumerate SF public records/rows
        // Create listener to find and list SF API endpoints
        // Create listener to find and sort discovered SF IDs

        this.log = montoyaApi.logging();
        this.api = montoyaApi;
        this.ui = new UserInterface(this);
        this.httpTap = new HttpTap(montoyaApi, this);
        montoyaApi.extension().setName("SFScan");
        montoyaApi.http().registerHttpHandler(httpTap);
        montoyaApi.userInterface().registerSuiteTab("SFScan", ui);
    }
}