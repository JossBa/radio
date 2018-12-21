package de.sb.radio.persistence;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SanityCheck {

	public static void main(String[] args) {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory("radio");
		EntityManager em = emf.createEntityManager();
		
		try{
			Person p = em.find(Person.class, 2L);
			System.out.println(p);
		}finally{
			em.close();
		}
	}

}
