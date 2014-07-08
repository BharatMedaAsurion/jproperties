package net.jmatrix.jproperties;

public class JPRuntimeException extends RuntimeException {

   public JPRuntimeException() {
   }

   public JPRuntimeException(String message) {
      super(message);
   }

   public JPRuntimeException(Throwable cause) {
      super(cause);
   }

   public JPRuntimeException(String message, Throwable cause) {
      super(message, cause);
   }

}
