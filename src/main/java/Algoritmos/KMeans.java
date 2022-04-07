package Algoritmos;

import Distancias.Distance;
import Distancias.DistanceFactory;
import Distancias.DistanceType;
import Distancias.Factory;
import Estructura.*;
import Exepciones.NotTrainedException;

public class KMeans implements  Algorithm<Table, String, Row>{

    private int numberClusters;
    private int iterations;
    private long seed;

    private RowWithLabels[] representatives;
    private TableWithLabels table;

    private int[] elementsOnCluster;

    private Distance distance;

    public KMeans(int numberClusters, int iterations, long seed, Distance distance) {
        this.numberClusters = numberClusters;
        elementsOnCluster = new int[numberClusters];
        restartCounter();
        this.iterations = iterations;
        this.seed = seed;
        representatives = new RowWithLabels[numberClusters];
        this.distance = distance;
    }

    @Override
    public void train(Table data) {
        //deal with empty data
        if (data == null || data.isEmpty()) return;
        // save given data
        table = (TableWithLabels) data;
        // choose representatives from data
        chooseRepresentatives();
        // repeat rest of steps
        for (int current_iteration = 0; current_iteration < iterations; current_iteration++) {
            // restart vector for counting elements on clusters
            restartCounter();
            // calculate the cluster of each element & assign tag + count how many elements per cluster there are
            for (int j =0; j < table.size(); j++) {
                RowWithLabels element = table.getRowAt(j);
                int elementCluster = calculateElementCluster(element);
                assignCluster(element, elementCluster);
                increaseClusterMembers(elementCluster);
            }
            // recalculate centroids
            recalculateRepresentatives();
        }
        // Finished training!
    }

    private int extractCluster(String label) {
        String num = label.replaceAll("[^0-9]","");
        return Integer.parseInt(num);
    }

    @Override
    public String estimate(Row data) {

        TableWithLabels repTable = new TableWithLabels();
        for (RowWithLabels row : representatives) repTable.addRow(row);

        KNearestNeighbours knn = new KNearestNeighbours(distance);
        knn.train(repTable);

        try {
            String guessedLabel = knn.estimate(data.getData());
            return guessedLabel;
        } catch (NotTrainedException e) {
            return "Not trained to guess";
        }
    }

    private void chooseRepresentatives() {
        for (int i = 0; i < numberClusters; i++) {
            int act = (int) (seed + i * seed % 1000) % this.table.size();
            //System.out.println(act);
            representatives[i] = this.table.getRowAt(act);
            representatives[i].addLabel("cluster-" + (i + 1));
            //System.out.println(Representatives[i]);
        }
    }

    private int calculateElementCluster(RowWithLabels element) {
        int elementCluster = 0;
        Double minDist = -1.0;
        Double distAct;
        int i = 0;
        for (RowWithLabels representative : representatives) {
            i++;
            distAct = distance.calculateDistance(element.getData(), representative.getData());
            //System.out.println(distAct);
            if (distAct < minDist || minDist < 0) {
                minDist = distAct;
                elementCluster = i;
                //System.out.println("new min ^^^");
            }
        }
        return elementCluster;
    }

    private void restartCounter() {
        for (int i = 0; i < numberClusters; i++) elementsOnCluster[i] = 0;
    }

    private void assignCluster(RowWithLabels element, int elementCluster) {
        element.addLabel("cluster-" + elementCluster);
    }

    private void increaseClusterMembers(int elementCluster) {
        elementsOnCluster[elementCluster - 1]++;
    }

    private void decreaseClusterMembers(int elementCluster) {
        elementsOnCluster[elementCluster - 1]--;
    }

    private void recalculateRepresentatives() {
        // redeclare Representatives and prepare for operations
        int regRowSize = representatives[0].size();
        for (int i = 0; i < numberClusters; i++) {
            representatives[i] = new RowWithLabels();
            representatives[i].addLabel("cluster-" + (i + 1));
            for (int o = 0; o < regRowSize; o++) representatives[i].addItem(0.0);
        }
        //System.out.println(Arrays.toString(Representatives));

        // add all data to respective representative
        for (int j =0; j < table.size(); j++) {
            RowWithLabels element = table.getRowAt(j);
            int clustNum = extractCluster(element.getLabel());
            for (int i = 0; i < regRowSize; i++) {
                Double newData = representatives[clustNum - 1].get(i) + element.get(i);
                representatives[clustNum - 1].set(newData, i);
            }
        }
        // mean data to the number of members it had
        for (int i = 0; i < numberClusters; i++) {
            for (int o = 0; o < regRowSize; o++) {
                Double fixedData = representatives[i].get(o) / elementsOnCluster[i];
                representatives[i].set(fixedData, o);
            }
        }

    }

}
