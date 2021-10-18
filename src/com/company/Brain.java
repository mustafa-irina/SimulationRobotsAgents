package com.company;

import jade.core.AID;
import jade.core.Agent;
import jade.core.AgentManager;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;

import java.util.Arrays;
import java.util.HashMap;

public class Brain extends Agent {
    //public int isTest = 0;
    public static int HOST = 3330;
    public BrainServer brainServer; //сервак для общение с роботом
    public LocalVotingAlgorithm localVotingAlgorithm;

    public int time_out = 32;

    public AMSAgentDescription[] agents = null; //доступные агенты


    public double [] [] matrix;
    public HashMap<AID, String> agentsData = new HashMap<AID, String>();
    public void setAgentsData(AID aid, String data) { agentsData.put(aid, data); }
    @Override
    protected void setup() {
        brainServer = new BrainServer("localhost", Brain.HOST, this.time_out);
        Brain.HOST++;
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    brainServer.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        server.setDaemon(true);
        server.start();


        this.agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults((long) -1);
            this.agents = AMSService.search(this, new AMSAgentDescription(), c);
            int n = this.agents.length;
            for (int i = 0; i < n; i++) {
                agentsData.put(this.agents[i].getName(), "-1");
            }
            this.matrix = new double[agents.length - 3][2];
        } catch (Exception e) {
            System.out.println("Problem searching AMS: " + e);
            e.printStackTrace();
        }

        this.localVotingAlgorithm= new LocalVotingAlgorithm();

        //ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
        //Behaviour rb = new RobotCommunication(this);
        Behaviour rbi = new RobotCommunicationInput(this, this.localVotingAlgorithm);
        Behaviour acr = new AgentsCommunication_Radio(this, this.localVotingAlgorithm);
        Behaviour acl = new AgentsCommunication_Listener(this, this.localVotingAlgorithm);
        Behaviour rbo = new RobotCommunicationOutput(this, this.localVotingAlgorithm);
//        addBehaviour(tbf.wrap(rb));
//        addBehaviour(tbf.wrap(acr));
//        addBehaviour(tbf.wrap(acl));

        addBehaviour(rbi);
        addBehaviour(acr);
        addBehaviour(acl);
        addBehaviour(rbo);

    }

    @Override
    protected void takeDown()
    {
        //System.exit(0);
    }

}

class RobotCommunicationInput extends CyclicBehaviour {
    BrainServer brainServer;
    String ROBOT_MESSAGE;
    Brain myAgentBrain;
    int time_out;
    LocalVotingAlgorithm algorithm;
    private double[] lightValues = new double[4];
    private double distValue;
    private double counter;
    private double avoidObstacleCounter;
    private double p; // - уверенность к курсу при пересчете от группы

    RobotCommunicationInput(Brain brain, LocalVotingAlgorithm algorithm) {//
        this.myAgentBrain = brain;
        this.brainServer = myAgentBrain.brainServer;
        this.time_out = myAgentBrain.time_out;
        this.algorithm = algorithm;
    }

    @Override
    public void action() {
        if (brainServer.isWorkServer() && brainServer.isRobotSentMessage()) {
            //System.out.println("is work server!");
            ROBOT_MESSAGE = brainServer.getRobotMessage();
            this.setConfidenceValue(ROBOT_MESSAGE);
        }
//        try {
//            Thread.sleep(time_out);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        block(time_out);
    }

    private void setConfidenceValue(String robot_message) {
        if (robot_message == null) {
            System.out.println(myAgent.getLocalName() + " get null");
            System.exit(0);
            return;
        }
        String [] buff = robot_message.split(";");
        if(buff.length == 8) {
            for (int i = 0; i < 4; i++) {
                this.lightValues[i] = Double.parseDouble(buff[i]);
            }
            this.distValue = Double.parseDouble(buff[4]);
            this.counter = Double.parseDouble(buff[5]);
            this.avoidObstacleCounter = Double.parseDouble(buff[6]);
            this.p = Double.parseDouble(buff[7]);
        }
        this.algorithm.initLocalVotingAlgorithm(this.lightValues, this.distValue, this.counter,
                this.avoidObstacleCounter, this.p);
    } //подсчет уверенности робота
}

class RobotCommunicationOutput extends CyclicBehaviour {
    BrainServer brainServer;
    String BRAIN_MESSAGE;
    Brain myAgentBrain;
    int time_out;
    LocalVotingAlgorithm algorithm;

    RobotCommunicationOutput(Brain brain, LocalVotingAlgorithm algorithm) {//
        this.myAgentBrain = brain;
        this.brainServer = myAgentBrain.brainServer;
        this.time_out = myAgentBrain.time_out;
        this.algorithm = algorithm;
    }

    @Override
    public void action() {
        if (brainServer.isWorkServer()) {
            BRAIN_MESSAGE = this.algorithm.generateMessageToRobot();
            brainServer.setBrainMessage(BRAIN_MESSAGE);
        }
//        try {
//            Thread.sleep(time_out);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        block(time_out);
    }
}

//class RobotCommunication extends CyclicBehaviour {
//    BrainServer brainServer;
//    String ROBOT_MESSAGE;
//    String BRAIN_MESSAGE;
//    Brain myAgentBrain;
//    int time_out;
//
//    RobotCommunication(Brain brain) {//
//        this.myAgentBrain = brain;
//        this.brainServer = myAgentBrain.brainServer;
//        this.time_out = myAgentBrain.time_out;
//    }
//
//    @Override
//    public void action() {
//        if (brainServer.isWorkServer()) {
//            System.out.println("is work server!");
//            ROBOT_MESSAGE = brainServer.getRobotMessage();
//            myAgentBrain.setConfidenceValue(ROBOT_MESSAGE);//считаем уверенность
//            //отправляем другим агентам
//            //получаем данные от других агентов
//            //алгоритм локального голосования -> новая увереннность
//            //считаем новый вектор движения -> BRAIN_MESSAGE
//            BRAIN_MESSAGE = myAgentBrain.LVA();
//            brainServer.setBrainMessage(BRAIN_MESSAGE);
//        }
//        try {
//            Thread.sleep(time_out);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        block(time_out);
//    }
//}

class AgentsCommunication_Radio extends CyclicBehaviour {
    Brain myAgentBrain;
    AMSAgentDescription[] agents = null;
    int n;
    int time_out;
    LocalVotingAlgorithm algorithm;
    BrainServer brainServer;

    AgentsCommunication_Radio(Brain brain, LocalVotingAlgorithm algorithm) {
        this.myAgentBrain = brain;
        this.agents = myAgentBrain.agents;
        this.n = this.agents.length;
        this.time_out = myAgentBrain.time_out;
        this.algorithm = algorithm;
        this.brainServer = myAgentBrain.brainServer;
    }

    @Override
    public void action() {

        if (brainServer.isWorkServer() && brainServer.isRobotSentMessage()) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent(this.algorithm.generateMessageToAgents());

            for(int i = 0; i < n; i++) {
                AID r = this.agents[i].getName();
                if (!r.equals(myAgent.getAID())) {
                    msg.addReceiver(r);
                }
            }
            //System.out.println(myAgent.getLocalName() + ": sending message to all --- " + msg.getContent());
            myAgent.send(msg);
        }
        block(time_out);

    }
}

class AgentsCommunication_Listener extends CyclicBehaviour {
    Brain myAgentBrain;
    AMSAgentDescription[] agents = null;
    int n;
    int time_out;
    double [] [] matrix;
    int k;
    int k_t;
    LocalVotingAlgorithm algorithm;
    BrainServer brainServer;

    AgentsCommunication_Listener(Brain brain, LocalVotingAlgorithm algorithm) {
        this.myAgentBrain = brain;
        this.agents = myAgentBrain.agents;
        this.n = this.agents.length;
        this.k_t = n - 3;
        this.k = n - 3;
        this.time_out = myAgentBrain.time_out;
        this.matrix = myAgentBrain.matrix;
        //this.matrix = new double[this - 3][2];
        this.algorithm = algorithm;
        this.brainServer = myAgentBrain.brainServer;
    }

    @Override
    public void action() {
        int j = 0;
        int i = 0;
        StringBuilder all = new StringBuilder("");
        if (brainServer.isWorkServer() && brainServer.isRobotSentMessage()) {
            while (i < k) {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    String msg_name = msg.getSender().getLocalName();
                    all.append(msg_name).append(" ");
                    if (!msg_name.equals(myAgent.getLocalName()) && msg_name.startsWith("Brain")) { //!msg_name.equalsIgnoreCase("df") && !msg_name.equalsIgnoreCase("ams") && !msg_name.equalsIgnoreCase(myAgentBrain.getName())
                        Double [] vector = Arrays.stream(msg.getContent().split(";")).map(Double::parseDouble).toArray(Double[]::new);
                        matrix[i][0] = vector[0];
                        matrix[i][1] = vector[1];
                        if (matrix[i][0] == -1 || matrix[i][1] == -1) {
                            k_t--;
                        }
                        myAgentBrain.setAgentsData(msg.getSender(), msg.getContent());
                        i++;
                    } else {
                        continue;
                    }
                } else {
                    matrix[i][0] = -1;
                    matrix[i][1] = -1;
                    i++;
                }
            }
            //System.out.println(all);
            this.algorithm.LVA(this.matrix, this.k, this.k_t);
            this.k_t = n - 3;
            this.k = n - 3;
        }
        block(time_out);
    }
}