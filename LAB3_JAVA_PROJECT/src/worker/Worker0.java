package worker;

import config.Config;
import algorithms.SortingAlgorithms;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Worker0 {

    public static void main(String[] args) {
        try {
            // Configurar servidor para recibir datos del cliente
            ServerSocket serverSocket = new ServerSocket(Config.WORKER_0_PORT);
            System.out.println("Worker0 esperando conexión en el puerto " + Config.WORKER_0_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Conexión recibida del cliente.");

                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                // Leer datos del cliente
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
                        vector = future.get(Config.TIME_LIMIT_WORKER_0, TimeUnit.MILLISECONDS);
                        long endTime = System.currentTimeMillis();

                        // Enviar resultados al cliente
                        out.writeObject(vector);
                        out.writeLong(endTime - startTime);
                        out.flush();
                        completado = true;
                        System.out.println("Vector procesado completamente por Worker0 y enviado al cliente.");
                    } catch (TimeoutException e) {
                        System.out.println("Tiempo límite excedido en Worker0, enviando datos a Worker1.");
                        future.cancel(true);

                        // Transferir datos a Worker1
                        try (Socket worker1Socket = new Socket(Config.WORKER_1_IP, Config.WORKER_1_PORT);
                             ObjectOutputStream workerOut = new ObjectOutputStream(worker1Socket.getOutputStream());
                             ObjectInputStream workerIn = new ObjectInputStream(worker1Socket.getInputStream())) {

                            // Enviar datos a Worker1
                            workerOut.writeObject(vector);
                            workerOut.writeInt(algoritmo);
                            workerOut.flush();

                            // Recibir resultados de Worker1
                            vector = (int[]) workerIn.readObject();
                            completado = workerIn.readBoolean(); // Worker1 indica si completó o no

                            if (completado) {
                                out.writeObject(vector);
                                out.writeLong(workerIn.readLong()); // Tiempo de procesamiento
                                out.flush();
                                System.out.println("Resultados recibidos de Worker1 y enviados al cliente.");
                            }
                        } catch (SocketException ex) {
                            System.out.println("Error al conectar con Worker1. Reintentando...");
                        }
                    }
                    executor.shutdown();
                }

                // Cerrar conexiones
                in.close();
                out.close();
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
