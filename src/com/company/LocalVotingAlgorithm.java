package com.company;

import java.util.Arrays;

import static java.lang.Math.abs;

public class LocalVotingAlgorithm {
    private double[] lights = new double[4]; //датчики света
    private double dist = 0; //датчик дистанции
    private double error = 0; //"азимут"
    private double light_max = 0;
    private double lignt_max2 = 0;
    private double q_conf = 0; //уверенность робота
    private double leftSpeed = 0;
    private double rightSpeed = 0;
    private double counter = 0;
    private double avoidObstacleCounter = 0;
    private double p = 0; // - уверенность к курсу при пересчете от группы

    public LocalVotingAlgorithm() {}

    public void initLocalVotingAlgorithm(double[] lights, double dist, double counter, double avoidObstacleCounter,
                                         double p) {
        this.lights = lights;
        this.dist = dist;
        this.counter = counter;
        this.avoidObstacleCounter = avoidObstacleCounter;
        this.p = p;
        light_max = Arrays.stream(lights).max().getAsDouble();

        if (light_max != 0) {

            if (light_max == lights[0]) { //если максимум 0

                if (lights[1] <= lights[3] && lights[3] != 0) {

                    lignt_max2 = lights[3];
                    error = (lights[0] * 90) / (lights[0] + lights[3]) - 45;

                } else if (lights[1] > lights[3] && lights[1] != 0) {

                    lignt_max2 = lights[1];
                    error = 45 + (lights[1]*90)/(lights[0]+lights[1]);

                } else { error = 45; }

            } else if (light_max == lights[1]) { //если максимум 1

                if (lights[2] <= lights[0] && lights[0] != 0) {

                    lignt_max2 = lights[0];
                    error = 45 + (lights[1] *90) / (lights[0] + lights[1]);

                } else if (lights[2] > lights[0] && lights[2] != 0) {

                    lignt_max2 = lights[2];
                    error = 135 + (lights[2]*90)/(lights[2]+lights[1]);

                } else { error = 135; }

            } else if (light_max == lights[2]) { //если максимум 2

                if (lights[3] <= lights[1] && lights[1] != 0) {

                    lignt_max2 = lights[1];
                    error = 135 + (lights[2]*90)/(lights[2]+lights[1]);

                } else if(lights[3] > lights[1] && lights[3] != 0) {

                    lignt_max2 = lights[3];
                    error = 225 + (lights[3]*90)/(lights[2]+lights[3]);

                } else { error = 225; }

            } else if (light_max == lights[3]) { //если максимум 3

                if (lights[0] <= lights[2] && lights[2] != 0) {

                    lignt_max2 = lights[2];
                    error = 225 + (lights[3]*90)/(lights[2]+lights[3]);

                } else if (lights[0] > lights[2] && lights[0] != 0) {

                    lignt_max2 = lights[0];
                    error = 315 + (lights[0]*90)/(lights[0]+lights[3]);

                } else { error = 315; }

            }
        }
        //end constructor
    }

    private void comp_conf() {
        double a_q = 0.5;
        if (lights[0] + lights[3] == 0) {
            q_conf = 0;
        } else {
            q_conf = (1-a_q)*(1 - abs((lights[0]-lights[3])/(lights[0]+lights[3]))) + a_q*(dist/1000);
        }
    }
    public double getQ_conf() { return q_conf; }
    public String generateMessageToAgents() {
        StringBuilder msg = new StringBuilder("");
        msg.append(error).append(";").append(q_conf).append(";");
        return msg.toString();
    }

    public void LVA(double [][] matrix_from_agent, int k, int k_t) {
        double alpha = 0.8;
        double deltaq = 0;
        double sigma_t = 0;
        for (double [] qm:
             matrix_from_agent) {
            if (qm [1] != -1) {
                deltaq += qm [1] - q_conf;
            }
        }
        sigma_t = k_t == 0 ? 0 : (alpha*deltaq)/k_t;
        if (q_conf == 0) { q_conf = 0.01; }
        double gamma_t = 1/(q_conf+sigma_t);

        double cos_delta_sum = 0;
        double sin_delta_sum = 0;
        double cos_error = Math.cos(Math.toRadians(error));
        double sin_error = Math.sin(Math.toRadians(error));

        for (double [] qm:
                matrix_from_agent) {
            if (qm [1] != -1) {
                double cos_bearingn = Math.cos(Math.toRadians(qm[0]));
                double sin_bearingn = Math.sin(Math.toRadians(qm[0]));
                cos_delta_sum += cos_bearingn*qm[1] - cos_error*q_conf;
                sin_delta_sum += sin_bearingn*qm[1] - sin_error*q_conf;
            }
         }

        double dbearingG = 0;
        if (k_t == 0) { dbearingG = error; } else  {
            double cos_dbearingG = cos_error*(1-(sigma_t*gamma_t))+(alpha*gamma_t*cos_delta_sum)/k_t;
            double sin_dbearingG = sin_error*(1-(sigma_t*gamma_t))+(alpha*gamma_t*sin_delta_sum)/k_t;


            if (cos_dbearingG < -1) { cos_dbearingG = -1; }

            if (cos_dbearingG > 1) { cos_dbearingG = 1; }

            if (cos_dbearingG > 0 && sin_dbearingG > 0) {
                dbearingG = Math.toDegrees(Math.acos(cos_dbearingG));
            } else if (cos_dbearingG > 0 && sin_dbearingG < 0) {
                dbearingG = 360 - Math.toDegrees(Math.acos(cos_dbearingG));
            } else if (cos_dbearingG < 0 && sin_dbearingG > 0) {
                dbearingG = 180 - Math.toDegrees(Math.acos(cos_dbearingG));
            } else if (cos_dbearingG < 0 && sin_dbearingG < 0) {
                dbearingG = 180 + Math.toDegrees(Math.acos(cos_dbearingG));
            }
        }

        if (dbearingG <= 5 || dbearingG >= 355) {
            leftSpeed = 3.14;
            rightSpeed = 3.14;
        } else if (dbearingG <= 175) {
            leftSpeed = 3.14;
            rightSpeed = 3.14*(1-dbearingG/180);
        } else if (dbearingG >= 185) {
            leftSpeed = 3.14*((dbearingG/180)-1);
            rightSpeed = 3.14;
        } else {
            counter = 50;
        }

        if (dist <= 600 && avoidObstacleCounter == 0) {
            avoidObstacleCounter = 1;
            if (lights[0] == lights[1] && lights[2] == lights[3] && lights[0] == lights[3] && lights[2] == lights[1]) {
                p = (int)(Math.random() * 2);
            } else if (light_max == lights[0] || light_max == lights[1]) {
                p = 0; //право
            } else if (light_max == lights[2] || light_max == lights[3]) {
                p = 1; //влево
            }
        }

        if (avoidObstacleCounter != 0) {
            if (dist > 600) {
                avoidObstacleCounter = 0;
            } else {
                avoidObstacleCounter -= 1;
                if (p == 1) {
                    leftSpeed = -2;
                    rightSpeed = 2;
                } else if (p == 0) {
                    leftSpeed = 2;
                    rightSpeed = -2;
                }

            }
        }
    }

    public String generateMessageToRobot() {
        StringBuilder msg = new StringBuilder("");
        msg.append(this.leftSpeed).append(";").
                append(this.rightSpeed).append(";").
                append(this.counter).append(";").
                append(this.avoidObstacleCounter).append(";").
                append(this.p);
        return msg.toString();
    }
}
