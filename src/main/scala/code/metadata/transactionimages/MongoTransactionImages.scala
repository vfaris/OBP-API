package code.metadata.transactionimages

import code.model.{User, TransactionImage}
import net.liftweb.common.{Loggable, Box}
import java.net.URL
import java.util.Date
import org.bson.types.ObjectId
import net.liftweb.mongodb.record.{MongoMetaRecord, MongoRecord}
import net.liftweb.mongodb.record.field.{DateField, ObjectIdPk}
import net.liftweb.record.field.{LongField, StringField}
import net.liftweb.util.Helpers._
import net.liftweb.common.Full
import com.mongodb.{DBObject, QueryBuilder}

private object MongoTransactionImages extends TransactionImages with Loggable {

  def getImagesForTransaction(bankId : String, accountId : String, transactionId: String)() : List[TransactionImage] = {
    OBPTransactionImage.findAll(bankId, accountId, transactionId)
  }
  
  def addTransactionImage(bankId : String, accountId : String, transactionId: String)
  (userId: String, viewId : Long, description : String, datePosted : Date, imageURL: URL) : Box[TransactionImage] = {
    OBPTransactionImage.createRecord.
      bankId(bankId).
      accountId(accountId).
      transactionId(transactionId).
      userId(userId).
      viewID(viewId).
      imageComment(description).
      date(datePosted).
      url(imageURL.toString).saveTheRecord()
  }
  
  def deleteTransactionImage(bankId : String, accountId : String, transactionId: String)(imageId : String) : Box[Unit] = {
    //use delete with find query to avoid concurrency issues
    OBPTransactionImage.delete(OBPTransactionImage.getFindQuery(bankId, accountId, transactionId, imageId))

    //we don't have any useful information here so just assume it worked
    Full()
  }
  
}

private class OBPTransactionImage private() extends MongoRecord[OBPTransactionImage]
with ObjectIdPk[OBPTransactionImage] with TransactionImage {
  def meta = OBPTransactionImage

  //These fields are used to link this to its transaction
  object transactionId extends StringField(this, 255)
  object accountId extends StringField(this, 255)
  object bankId extends StringField(this, 255)

  object userId extends StringField(this,255)
  object viewID extends LongField(this)
  object imageComment extends StringField(this, 1000)
  object date extends DateField(this)
  object url extends StringField(this, 500)

  def id_ = id.is.toString
  def datePosted = date.get
  def postedBy = User.findByApiId(userId.get)
  def viewId = viewID.get
  def description = imageComment.get
  def imageUrl = {
    tryo {new URL(url.get)} getOrElse OBPTransactionImage.notFoundUrl
  }
}

private object OBPTransactionImage extends OBPTransactionImage with MongoMetaRecord[OBPTransactionImage] {
  val notFoundUrl = new URL("http://google.com" + "/notfound.png") //TODO: Make this image exist?

  def findAll(bankId : String, accountId : String, transactionId : String) : List[OBPTransactionImage] = {
    val query = QueryBuilder.start("bankId").is(bankId).put("accountId").is(accountId).put("transactionId").is(transactionId).get
    findAll(query)
  }

  //in theory commentId should be enough as we're just using the mongoId
  def getFindQuery(bankId : String, accountId : String, transactionId : String, imageId : String) : DBObject = {
    QueryBuilder.start("_id").is(new ObjectId(imageId)).put("transactionId").is(transactionId).
      put("accountId").is(accountId).put("bankId").is(bankId).get()
  }
}