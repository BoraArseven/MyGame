package com.boracompany.mygame.ORM;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import com.boracompany.mygame.model.Player;
import com.boracompany.mygame.orm.PlayerDAOIMPL;

public class TestPlayerDAOIMPL extends PlayerDAOIMPL {

    public TestPlayerDAOIMPL(EntityManagerFactory emf) {
        super(emf);
    }

    @Override
    public void updatePlayer(Player player) {
        EntityManager em = getEmf().createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            if ("Should Rollback".equals(player.getName())) {
                throw new RuntimeException("Failed to update player due to an unexpected error.");
            }
            em.merge(player);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
    @Override
    public void deletePlayer(Player player) {
        EntityManager em = getEmf().createEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = em.getTransaction();
            transaction.begin();

            Player managedPlayer = em.find(Player.class, player.getId());
            
            if (managedPlayer != null) {
                if ("Should Rollback".equals(managedPlayer.getName())) {
                    throw new RuntimeException("Failed to update player due to an unexpected error.");
                }
                em.remove(managedPlayer);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            throw e;
        } finally {
            em.close();
        }
    }
    
}