package com.campusfix.bootstrap;

/** Punto de entrada: arranca CampusFix en el puerto PORT (8080 por defecto). */
public final class CampusFixApp {

    private CampusFixApp() {
    }

    public static void main(String[] args) throws Exception {
        Aplicacion app = Aplicacion.iniciar(Aplicacion.Config.porDefecto());
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        System.out.println("CampusFix escuchando en http://localhost:" + app.puerto());
        Thread.currentThread().join();
    }
}
