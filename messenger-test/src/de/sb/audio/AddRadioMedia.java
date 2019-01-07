package de.sb.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import de.sb.radio.persistence.Album;
import de.sb.radio.persistence.Document;
import de.sb.radio.persistence.Person;
import de.sb.radio.persistence.Track;

public class AddRadioMedia {

	public static void main(String[] args) throws IOException {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory("radio");
		try {
			final EntityManager em = emf.createEntityManager();
			try {	
				em.getTransaction().begin();
				
				final Path audioPath = Paths.get("/Users/Nadavbabai/Documents/HTW/4/Projekte/RadioData/Cigarettes After Sex - Nothings Gonna Hurt You Baby.mp3");
				final Path coverPath = Paths.get("/Users/Nadavbabai/Documents/HTW/4/Projekte/RadioData/Cigarettes After Sex - Nothings Gonna Hurt You Baby.png");

				Person owner = em.find(Person.class, 5L);
				if(owner == null) throw new IllegalStateException();
				
				Document audio = new Document();
				audio.setContent(Files.readAllBytes(audioPath));
				audio.setContentType("audio/mp3");
				em.persist(audio);
				Document  cover = new Document();
				cover.setContent(Files.readAllBytes(coverPath));
				cover.setContentType("image/png");
				em.persist(cover);

				try {
					em.getTransaction().commit();
				} finally {
					em.getTransaction().begin();
				}
				
				Album album = new Album(cover);
				album.setTitle("I.");
				album.setTrackCount((byte) 4);
				album.setReleaseYear((short) 2012);
				em.persist(album);
				try {
					em.getTransaction().commit();
				} finally {
					em.getTransaction().begin();
				}
			
				Track track = new Track(audio, album, owner);
				track.setGenre("Indie");
				track.setName("Nothing's Gonna Hurt You Baby");
				track.setArtist("Cigarettes After Sex");
				track.setOrdinal((byte) 1); 
				em.persist(track);
				em.getTransaction().commit();
			} finally {
				if(em.getTransaction().isActive()) em.getTransaction().rollback();
				em.close();
			}
		} finally {
			emf.close();	
		} 
	}
}
