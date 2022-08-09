package de.infynyty.wokoupdates;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.IOException;
import java.net.URI;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Logger;

@Log
public abstract class InsertionHandler<Insertion extends de.infynyty.wokoupdates.Insertion> {

    private final static long MAIN_CHANNEL_ID = 1002178166112137249L;
    private final static long LOG_CHANNEL_ID = 1004378115751030885L;

    private final ArrayList<Insertion> currentInsertions = new ArrayList<>();

    private final JDA jda;
    private final Dotenv dotenv;

    protected InsertionHandler(final JDA jda, final Dotenv dotenv) {
        this.jda = jda;
        this.dotenv = dotenv;
    }

    protected abstract String pullUpdatedHTML() throws IOException, InterruptedException;

    protected abstract ArrayList<Insertion> getInsertionsFromHTML(final String html);

    protected void updateCurrentInsertions() {

        final ArrayList<Insertion> updatedInsertions;
        try {
            updatedInsertions = new ArrayList<>(getInsertionsFromHTML(pullUpdatedHTML()));
        } catch (IOException e) {
            log.severe("An IO exception occurred while trying to update the insertions.");
            log.severe(e.getMessage());
            return;
        } catch (InterruptedException e) {
            log.severe("A Runtime exception occurred while trying to update the insertions.");
            log.severe(e.getMessage());
            return;
        }

        if (currentInsertions.isEmpty()) {
            currentInsertions.addAll(updatedInsertions);
            log.info("Initial download of all insertions completed successfully!");
            jda.getChannelById(TextChannel.class, LOG_CHANNEL_ID).sendMessage(
                "Initial download of all insertions completed successfully!"
            ).queue();
            currentInsertions.forEach(insertion -> System.out.println(insertion.toString()));
        }

        // go through all new insertions and check whether they are contained in the current insertions
        // update, if that isn't the case
        for (final Insertion updatedInsertion : updatedInsertions) {
            if (!(currentInsertions.contains(updatedInsertion))) {
                currentInsertions.add(updatedInsertion);
                log.info("New insertion found:\n\n" + updatedInsertion.toString());
                jda.getChannelById(TextChannel.class, MAIN_CHANNEL_ID).sendMessage(
                    "**New insertion found:**\n" + updatedInsertion
                ).queue();
                if(updatedInsertion.isNextTenantWanted() && updatedInsertion.getRent() < 650) {
                    jda.getChannelById(TextChannel.class, MAIN_CHANNEL_ID).sendMessage(dotenv.get("PING")).queue();
                }
            }
        }

        // go through all current insertions and check that they are still in the updated insertions
        // remove them, if that isn't the case
        final boolean wasRemoved = currentInsertions.removeIf(
            currentInsertion -> (!(updatedInsertions.contains(currentInsertion)))
        );
        if (wasRemoved) {
            log.info("One or more insertions were removed.");
            jda.getChannelById(TextChannel.class, LOG_CHANNEL_ID).sendMessage(
                "One or more insertions were removed."
            ).queue();
        }

        log.info(
            "Insertions updated at " + Date.from(Instant.now()) + ", numbers of insertions: " + updatedInsertions.size()
        );
        jda.getChannelById(TextChannel.class, LOG_CHANNEL_ID).sendMessage(
            "Insertions updated at " + Date.from(Instant.now()) + ", numbers of insertions: " + updatedInsertions.size()
        ).queue();
    }
}
