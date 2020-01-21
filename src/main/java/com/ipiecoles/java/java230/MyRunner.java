package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    private static final String REGEX_TYPE = "^[MTC]{1}.*";

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<Employe>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception { //throws Exception
        String fileName = "employes.csv";
        readFile(fileName);
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     *
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        //TODO
        //System.out.println(ligne);
        String[] ligneToTab = ligne.split(",");

        if (!ligne.matches(REGEX_TYPE)) {
            throw new BatchException("Type d'employé inconnu : " + ligne.charAt(0));
        }

        verifNbElement("M", ligneToTab);

        String date = ligneToTab[3];
        try {
            DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(date);
        } catch (Exception e) {
            throw new BatchException(date + " ne respecte pas le formation de date dd/MM/yyyy");
        }

        String salaire = ligneToTab[4];
        try {
            Double.parseDouble(salaire);
        } catch (Exception e) {
            throw new BatchException(salaire + " n'est pas un nombre valide pour un salaire");
        }

        verifNbElement("C", ligneToTab);

        if (String.valueOf(ligneToTab[0].toUpperCase().charAt(0)).equals("C")) {
            String salaireCom = ligneToTab[5];
            try {
                Double.parseDouble(salaireCom);
            } catch (Exception e) {
                throw new BatchException("Le chiffre d'affaire du commercial est incorrect");
            }

            String perfCom = ligneToTab[6];
            try {
                Double.parseDouble(perfCom);
            } catch (Exception e) {
                throw new BatchException("La performance du commercial est incorrecte");
            }
        }

        verifNbElement("T", ligneToTab);

        if (String.valueOf(ligneToTab[0].toUpperCase().charAt(0)).equals("T")) {
            String gradeTech = ligneToTab[5];
            try {
                Integer.parseInt(gradeTech);
            } catch (Exception e) {
                throw new BatchException("Le grade du technicien est incorrect : " + gradeTech);
            }

            if (!gradeTech.matches("[1-5]")) {
                throw new BatchException("Le grade doit être compris entre 1 et 5 : " + gradeTech);
            }

            String matManager = ligneToTab[6];
            if (!matManager.matches(REGEX_MATRICULE_MANAGER)) {
                throw new BatchException("la chaine " + matManager + " ne respecte pas l'expression régulière ^M[0-9]{5}$");
            }

            try {
                Employe technicien = employeRepository.findByMatricule(matManager);
                technicien.toString();
            } catch (Exception e) {
                throw new BatchException("Le manager de matricule " + matManager + " n'a pas été trouvé dans le fichier ou en base de données");
            }
            /*
            Employe technicien = employeRepository.findByMatricule(matManager);
            if (!technicien) {
                throw new BatchException("Le manager de matricule " + matManager + " n'a pas été trouvé dans le fichier ou en base de données");
            }*/
        }


        if (!ligneToTab[0].matches(REGEX_MATRICULE)) {
            throw new BatchException("La chaine " + ligneToTab[0] + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$");
        }


    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     *
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) throws Exception {
        Stream<String> stream;
        stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        //TODO
        Integer i = 0;
        for (String ligne : stream.collect(Collectors.toList())) {
            i++;
            try {
                processLine(ligne);
            } catch (BatchException e) {
                System.out.println("Ligne " + i + " : " + e.getMessage() + " => " + ligne);
            }
        }
        return employes;
    }

    /**
     * Méthode qui vérifie le nombre d'éléments séparés par une virgule sur une ligne
     *
     * @param lettre     lettre à tester
     * @param ligneToTab ligne tranformée en tableau (split par ",")
     */
    private void verifNbElement(String lettre, String[] ligneToTab) throws BatchException {
        String emp = "";
        int nb_champs = 0;
        switch (lettre) {
            case "M":
                emp = "manager";
                nb_champs = NB_CHAMPS_MANAGER;
                break;
            case "C":
                emp = "commercial";
                nb_champs = NB_CHAMPS_COMMERCIAL;
                break;
            case "T":
                emp = "technicien";
                nb_champs = NB_CHAMPS_TECHNICIEN;
                break;
        }
        if (String.valueOf(ligneToTab[0].toUpperCase().charAt(0)).equals(lettre) && ligneToTab.length != nb_champs) {
            throw new BatchException("La ligne " + emp + " ne contient pas " + nb_champs + " éléments mais " + ligneToTab.length);
        }
    }


    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     *
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        //TODO
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     *
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        //TODO
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     *
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        //TODO
    }

}
