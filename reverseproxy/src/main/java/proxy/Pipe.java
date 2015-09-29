package proxy;




import org.apache.http.MalformedChunkCodingException;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.util.ByteBufferAllocator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Pipe {

    /** IOControl of the reader */
    private IOControl producerIoControl;

    /** IOControl of the consumer */
    private IOControl consumerIoControl;

    /** Fixed size buffer to read and write data */
    private ByteBuffer buffer;



    private boolean producerCompleted = false;



    /** Lock to synchronize the producers and consumers */
    private Lock lock = new ReentrantLock();

    private Condition readCondition = lock.newCondition();
    private Condition writeCondition = lock.newCondition();

    /** Name to identify the buffer */
    private String name = "Buffer";

    private boolean consumerError = false;

    private boolean producerError = false;




    private boolean hasHttpProducer = true;

    private AtomicBoolean inBufferInputMode = new AtomicBoolean(true);


    public Pipe(IOControl producerIoControl, ByteBufferAllocator byteBufferAllocator , int size) {
        this.producerIoControl = producerIoControl;
        this.buffer = byteBufferAllocator.allocate(size);
        this.name += "_" + name;
    }

    public Pipe(ByteBuffer buffer, String name) {
        this.buffer = buffer;
        this.name += "_" + name;
        this.hasHttpProducer = false;
    }

    /**
     * Set the consumers IOControl
     * @param consumerIoControl IOControl of the consumer
     */
    public void attachConsumer(IOControl consumerIoControl) {
        this.consumerIoControl = consumerIoControl;
    }

    /**
     * Consume the data from the buffer. Before calling this method attachConsumer
     * method must be called with a valid IOControl.
     *
     * @param encoder encoder used to write the data means there will not be any data
     * written in to this buffer
     * @return number of bytes written (consumed)
     * @throws java.io.IOException if an error occurred while consuming data
     */
    public int consume(final ContentEncoder encoder) throws IOException {
        if (consumerIoControl == null) {
            throw new IllegalStateException("Consumer cannot be null when calling consume");
        }

        if (hasHttpProducer && producerIoControl == null) {
            throw new IllegalStateException("Producer cannot be null when calling consume");
        }

        lock.lock();
        ByteBuffer consumerBuffer;
        AtomicBoolean inputMode;

            consumerBuffer = buffer;
            inputMode = inBufferInputMode;

        try {
            // if producer at error we have to stop the encoding and return immediately
            if (producerError) {
                encoder.complete();
                return -1;
            }

            setOutputMode(consumerBuffer, inputMode);
            int bytesWritten = encoder.write(consumerBuffer);
            setInputMode(consumerBuffer, inputMode);

            if (consumerBuffer.position() == 0) {

                    if (producerCompleted) {
                        encoder.complete();
                    } else {
                        // buffer is empty. Wait until the producer fills up
                        // the buffer
                        consumerIoControl.suspendOutput();
                    }
            }

            if (bytesWritten > 0) {
                if (!encoder.isCompleted() && !producerCompleted && hasHttpProducer) {
                    producerIoControl.requestInput();
                }
                writeCondition.signalAll();
            }

            return bytesWritten;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Produce data in to the buffer.
     *
     * @param decoder decoder to read bytes from the underlying stream
     * @return bytes read (consumed)
     * @throws IOException if an error occurs while reading data
     */
    public int produce(final ContentDecoder decoder) throws IOException {
        if (producerIoControl == null) {
            throw new IllegalStateException("Producer cannot be null when calling produce");
        }

        lock.lock();
        try {
            setInputMode(buffer, inBufferInputMode);
            int bytesRead=0;
            try{
                bytesRead = decoder.read(buffer);
            } catch(MalformedChunkCodingException ignore) {
                // we assume that this is a truncated chunk, hence simply ignore the exception
                // https://issues.apache.org/jira/browse/HTTPCORE-195
                // we should add the EoF character
                buffer.putInt(-1);
                // now the buffer's position should give us the bytes read.
                bytesRead = buffer.position();

            }

            // if consumer is at error we have to let the producer complete
            if (consumerError) {
                buffer.clear();
            }

            if (!buffer.hasRemaining()) {
                // Input buffer is full. Suspend client input
                // until the origin handler frees up some space in the buffer
                producerIoControl.suspendInput();
            }

            // If there is some content in the input buffer make sure consumer output is active
            if (buffer.position() > 0 || decoder.isCompleted()) {
                if (consumerIoControl != null) {
                    consumerIoControl.requestOutput();
                }
                readCondition.signalAll();
            }

            if (decoder.isCompleted()) {
                producerCompleted = true;
            }
            return bytesRead;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public void consumerError() {
        lock.lock();
        try {
            this.consumerError = true;
            writeCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void producerError() {
        lock.lock();
        try {
            this.producerError = true;
            readCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }






    private void setInputMode(ByteBuffer buffer, AtomicBoolean inputMode) {
        if (inputMode.compareAndSet(false, true)) {
            if (buffer.hasRemaining()) {
                buffer.compact();
            } else {
                buffer.clear();
            }
        }
    }

    private void setOutputMode(ByteBuffer buffer, AtomicBoolean inputMode) {
        if (inputMode.compareAndSet(true, false)) {
            buffer.flip();
        }
    }









}

