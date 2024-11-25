package worker;

import config.Config;
import algorithms.SortingAlgorithms;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Worker1 {

    public static void main(String[] args) {
        try {
            // Configurar servidor para recibir datos de Worker0
            ServerSocket serverSocket = new ServerSocket(Config.WORKER_1_PORT);
            System.out.println("Worker1 esperando conexión en el puerto " + Config.WORKER_1_PORT);

            while (true) {
                Socket worker0Socket = serverSocket.accept();
                System.out.println("Conexión recibida de Worker0.");

                ObjectInputStream in = new ObjectInputStream(worker0Socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(worker0Socket.getOutputStream());

                // Leer datos de Worker0
                int[] vector = (int[]) in.readObject();
                int algoritmo = in.readInt();

                boolean completado = false;

                while (!completado) {
                    // Intentar procesar dentro del TIME_LIMIT
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    final int[] vectorFinal = vector; // Hacer el vector efectivamente final

                    Future<int[]> future = executor.submit(() -> {
                        switch (algoritmo) {
                            case 1 -> {
                                return SortingAlgorithms.heapsort(vectorFinal);
                            }
                            case 2 -> {
                                return SortingAlgorithms.quicksort(vectorFinal, 0, vectorFinal.length - 1);
                            }
                            case 3 -> {
                                return SortingAlgorithms.mergesort(vectorFinal);
                            }
                            default -> throw new IllegalArgumentException("Algoritmo no válido");
                        }
                    });

                    try {
                        long startTime = System.currentTimeMillis();
                        vector = future.get(Config.TIME_LIMIT_WORKER_1, TimeUnit.SECONDS);
                        long endTime = System.currentTimeMillis();

                        // Indicar que el procesamiento fue completado
                        out.writeObject(vector);
                        out.writeBoolean(true); // Indica que se completó
                        out.writeLong(endTime - startTime);
                        out.flush();
                        completado = true;
                        System.out.println("Vector procesado completamente por Worker1 y enviado a Worker0.");
                    } catch (TimeoutException e) {
                        System.out.println("Tiempo límite excedido en Worker1, enviando datos de regreso a Worker0.");
                        future.cancel(true);

                        // Transferir datos de regreso a Worker0
                        try (Socket worker0ReturnSocket = new Socket(Config.WORKER_0_IP, Config.WORKER_0_PORT);
                             ObjectOutputStream workerOut = new ObjectOutputStream(worker0ReturnSocket.getOutputStream());
                             ObjectInputStream workerIn = new ObjectInputStream(worker0ReturnSocket.getInputStream())) {

                            // Enviar datos de regreso a Worker0
                            workerOut.writeObject(vector);
                            workerOut.writeInt(algoritmo);
                            workerOut.flush();

                            // Recibir resultados de Worker0
                            vector = (int[]) workerIn.readObject();
                            completado = workerIn.readBoolean(); // Worker0 indica si completó o no

                            if (completado) {
                                out.writeObject(vector);
                                out.writeBoolean(true);
                                out.writeLong(workerIn.readLong()); // Tiempo de procesamiento
                                out.flush();
                                System.out.println("Resultados recibidos de Worker0 y enviados al cliente.");
                            }
                        }
                    }
                    executor.shutdown();
                }

                // Cerrar conexiones
                in.close();
                out.close();
                worker0Socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
