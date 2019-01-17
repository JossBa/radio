package de.sb.radio.persistence;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import de.sb.radio.persistence.BaseEntity;


public class SanityCheck {
	static public void main (final String[] args) {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("radio");
		EntityManager em = emf.createEntityManager();
		em.find(BaseEntity.class, 1L);
	}
}