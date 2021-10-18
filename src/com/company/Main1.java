package com.company;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Main1 {
    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "127.0.0.1");
        profile.setParameter(Profile.MAIN_PORT, "8080");
        profile.setParameter(Profile.GUI, "false");

        ContainerController containerController = runtime.createMainContainer(profile);

        try {
            for (int i = 0; i < 11; i++) {
                AgentController brain;
                brain = containerController.createNewAgent("Brain" + i, "com.company.Brain", null);
                brain.start();
            }

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
