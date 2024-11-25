package client;

import config.Config;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

    // Leer un vector desde un archivo de texto
    public static int[] leerVectorDesdeArchivo(String rutaArchivo) throws IOException {
        List<Integer> listaNumeros = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                listaNumeros.add(Integer.parseInt(linea.trim()));
            }
        }
        int[] vector = new int[listaNumeros.size()];
        for (int i = 0; i < listaNumeros.size(); i++) {
            vector[i] = listaNumeros.get(i);
        }
        return vector;
    }

    // Mostrar menú para seleccionar algoritmo
    public static int mostrarMenu() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Seleccione el algoritmo de ordenamiento:");
        System.out.println("1. Heapsort");
        System.out.println("2. Quicksort");
        System.out.println("3. Mergesort");
        System.out.print("Ingrese su opción: ");
        return scanner.nextInt();
    }

    // Método principal
    public static void main(String[] args) {
        try {
            // Pedir la ruta del archivo
            Scanner scanner = new Scanner(System.in);
            System.out.print("Ingrese la ruta del archivo de texto: ");
            String rutaArchivo = scanner.nextLine();

            // Leer el vector desde el archivo
            int[] vector = leerVectorDesdeArchivo(rutaArchivo);

            // Seleccionar algoritmo
            int opcion = mostrarMenu();

            boolean completado = false;
            boolean enWorker0 = true; // Alternar entre Worker0 y Worker1

            while (!completado) {
                try {
                    if (enWorker0) {
                        System.out.println("Enviando datos al worker_0...");
                        try (Socket socket = new Socket(Config.WORKER_0_IP, Config.WORKER_0_PORT);
                             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                            // Enviar datos al worker_0
                            out.writeObject(vector);
                            out.writeInt(opcion);
                            out.flush();

                            // Recibir resultados de worker_0
                            vector = (int[]) in.readObject();
                            completado = in.readBoolean(); // Indica si terminó

                            if (completado) {
                                long tiempoResolucion = in.readLong();
                                System.out.println("Vector ordenado recibido desde worker_0:");
                                System.out.println(Arrays.toString(vector));
                                System.out.println("Tiempo total: " + tiempoResolucion + " ms");
                            }
                        }
                    } else {
                        System.out.println("Enviando datos al worker_1...");
                        try (Socket socket = new Socket(Config.WORKER_1_IP, Config.WORKER_1_PORT);
                             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                            // Enviar datos al worker_1
                            out.writeObject(vector);
                            out.writeInt(opcion);
                            out.flush();

                            // Recibir resultados de worker_1
                            vector = (int[]) in.readObject();
                            completado = in.readBoolean(); // Indica si terminó

                            if (completado) {
                                long tiempoResolucion = in.readLong();
                                System.out.println("Vector ordenado recibido desde worker_1:");
                                System.out.println(Arrays.toString(vector));
                                System.out.println("Tiempo total: " + tiempoResolucion + " ms");
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Error de conexión. Cambiando de worker...");
                }

                // Alternar al siguiente worker si no ha completado
                enWorker0 = !enWorker0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
