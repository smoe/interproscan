package uk.ac.ebi.interpro.scan.jms.worker;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import uk.ac.ebi.interpro.scan.jms.SessionHandler;
import uk.ac.ebi.interpro.scan.management.model.Jobs;
import uk.ac.ebi.interpro.scan.management.model.StepExecution;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.net.UnknownHostException;

/**
 * TODO: Add description of class.
 *
 * @author Phil Jones
 * @version $Id: TestWorkerMonitor.java,v 1.5 2009/10/26 12:06:56 pjones Exp $
 * @since 1.0
 */
public class InterProScanMonitor implements WorkerMonitor {

    private static final Logger LOGGER = Logger.getLogger(InterProScanMonitor.class);

    private long startTimeMillis = System.currentTimeMillis();

    private String workerManagerTopicName;

    private String workerManagerResponseQueueName;

    private String jmsBrokerHostName;

    private int jmsBrokerPort;

    private volatile Worker worker;

    private Jobs jobs;

    @Required
    public void setJmsBrokerHostName(String jmsBrokerHostName) {
        this.jmsBrokerHostName = jmsBrokerHostName;
    }

    @Required
    public void setJmsBrokerPort(int jmsBrokerPort) {
        this.jmsBrokerPort = jmsBrokerPort;
    }

    /**
     * Sets the name of the worker manager topic.  This is a topic
     * that is used to poll for the status of all Worker clients.
     *
     * @param workerManagerTopicName the name of the worker manager topic.
     */
    @Override
    public void setWorkerManagerTopicName(String workerManagerTopicName) {
        this.workerManagerTopicName = workerManagerTopicName;
    }

    /**
     * Sets the name of the worker manager response queue.
     * <p/>
     * This queue is used to return responses to requests for the
     * status of the Worker client.
     *
     * @param workerManagerResponseQueueName the name of the worker manager response queue.
     */
    @Override
    public void setWorkerManagerResponseQueueName(String workerManagerResponseQueueName) {
        this.workerManagerResponseQueueName = workerManagerResponseQueueName;
    }

    /**
     * Called by the Worker that this WorkerManager is injected into
     * so that the WorkerManager can run management tasks on the Worker.
     */
    @Override
    public void setWorker(Worker worker) {
        this.worker = worker;
    }


    public void setJobs(Jobs jobs) {
        this.jobs = jobs;
    }

    /**
     * This run method should be run as a high priority thread.
     *
     * Subscribes to the WorkerManagerTopic and should react quickly to any messages
     * published on this topic.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (worker == null){
            throw new IllegalStateException("The Worker being managed by this WorkerMonitor must be set before running this thread.");
        }
        SessionHandler sessionHandler = null;
        try{
            sessionHandler = new SessionHandler(jmsBrokerHostName, jmsBrokerPort);
            MessageConsumer messageConsumer = sessionHandler.getMessageConsumer(workerManagerTopicName);
            MessageProducer messageProducer = sessionHandler.getMessageProducer(workerManagerResponseQueueName);
            // Responses to monitor requests shouldn't hang around in the queue too long and
            // don't need to be persisted in the queue database.
            messageProducer.setTimeToLive(20000);
            // Consume a single message off the response queue and handle it.
            // (or die if this does not occur in 20 seconds)
            while (worker.isRunning()){
                TextMessage managementRequest = (TextMessage) messageConsumer.receive(20000);
                // Just echo back the managementRequest with the name of the worker host to uk.ac.ebi.interpro.scan.jms the multicast topic.
                if (managementRequest != null){   // receive has received a managementRequest before timing out.
                    WorkerState workerState = new WorkerState(
                            System.currentTimeMillis() - startTimeMillis,
                            java.net.InetAddress.getLocalHost().getHostName(),
                            worker.getWorkerUniqueIdentification(),
                            worker.isStopWhenIdle()
                    );
                    workerState.setJobId("Unique Job ID as passed from the broker in the JMS header. (TODO)");
                    workerState.setProportionComplete(worker.getProportionOfWorkDone());
                    workerState.setWorkerStatus((worker.isRunning()) ? "Running" : "Not Running");
                    StepExecution stepExecution = worker.getCurrentStepExecution();
                    if (stepExecution == null){
                        workerState.setStepExecutionState(null);
                        workerState.setJobId("-");
                        workerState.setJobDescription("-");
                    }
                    else {
                        workerState.setStepExecutionState(stepExecution.getState());
                        workerState.setJobId(stepExecution.getId().toString());
                        workerState.setJobDescription(stepExecution.getStepInstance().getStep(jobs).getStepDescription());
                    }
                    ObjectMessage responseObject = sessionHandler.createObjectMessage(workerState);

                    // Find out if there is a 'requestee' header, which should be added to the response message
                    // so the management responses can be filtered out by the client that sent them.
                    if (managementRequest.propertyExists(WorkerMonitor.REQUESTEE_PROPERTY)){
                        responseObject.setStringProperty(WorkerMonitor.REQUESTEE_PROPERTY,
                                managementRequest.getStringProperty(WorkerMonitor.REQUESTEE_PROPERTY));
                    }

                    messageProducer.send(responseObject);
                    managementRequest.acknowledge();
                }
            }
        } catch (JMSException e) {
            LOGGER.error ("JMSException thrown by InterProScanMonitor", e);
        }
        catch (UnknownHostException uhe){
            LOGGER.error ("UnknownHostException thrown by InterProScanMonitor", uhe);
        }
        finally {
            try {
                if (sessionHandler != null){
                    sessionHandler.close();
                }
            } catch (JMSException e) {
                LOGGER.error ("JMSException thrown by InterProScanMonitor when attempting to close Session / Connection.", e);
            }
        }
    }
}
