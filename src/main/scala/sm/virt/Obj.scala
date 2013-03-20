package sm.virt

import collection.mutable
import sm._
import sm.imm.Field
import sm.imm.Access
import sm.imm
import scala.Some
import reflect.ClassTag
import sm.imm.Type.CharClass



object Obj{

  def initMembers(cls: imm.Cls, filter: Field => scala.Boolean)(implicit vm: VM): List[Map[String, virt.Val]] = {
    import vm._
    cls.fields.filter(filter).map{f =>
      f.name -> imm.Type.CharClass.default(f.desc)
    }.toMap :: cls.superType.toList.flatMap(x => initMembers(x.clsData, filter))
  }


  def apply(clsName: String, initMembers: (String, virt.Val)*)(implicit vm: VM) = {
    new Obj(vm.Classes(imm.Type.Cls(clsName)), initMembers: _*)
  }
  def unapply(x: Any) = x match{
    case x: Obj => Some((x.cls.name, x.members))
    case _ => None
  }
}
class Obj(val cls: sm.Cls, initMembers: (String, virt.Val)*)
         (implicit vm: VM) extends StackVal with Cat1{ import vm._

  val members =
    Obj.initMembers(cls.clsData, x => (x.access & Access.Static) == 0)
       .map(x => mutable.Map(x.toSeq:_*))
  val magicMembers = mutable.Map[String, Any]()
  for ((s, v) <- initMembers){
    this(imm.Type.Cls.read(cls.name), s) = v
  }

  def apply(owner: imm.Type.Cls, name: String): virt.Val = {
    val start = cls.ancestry.indexWhere(_.tpe == owner)

    members.drop(start)
           .find(_.contains(name))
           .get(name)

  }
  def update(owner: imm.Type.Cls, name: String, value: virt.Val) = {
    val start = cls.ancestry.indexWhere(_.tpe == owner)

    members.drop(start)
           .find(_.contains(name))
           .get(name) = value
  }

  def withMagic(x: String, a: Any) = {
    magicMembers(x) = a
    this
  }
  override def toString = {
    s"virt.Obj(${cls.name})"
  }
}

trait Arr extends StackVal with Cat1{
  val tpe: imm.Type.Entity
  val backing: Array[_]
}
object ObjArr{
  class TypeX[T](val t: imm.Type)

  def apply(t: imm.Type.Entity, n: scala.Int) = {
    new ObjArr(t, Array.fill[virt.Val](n)(imm.Type.CharClass.default(t)))
  }
}
class ObjArr(val tpe: imm.Type.Entity, val backing: Array[virt.Val]) extends Arr{
  override def toString = s"virt.ObjArr(${tpe.unparse}: ${backing.fold("")(_+", "+_)})"
}
object PrimArr{
  def apply[T: ClassTag: CharClass](n: scala.Int) = {
    new PrimArr(new Array[T](n))
  }
  def unapply(s: virt.Val) = s match{
    case x: virt.PrimArr[_] => Some(x.backing)
    case _ => None
  }

}
class PrimArr[T: CharClass](val backing: Array[T]) extends Arr{
  lazy val tpe: imm.Type.Prim = CharClass()
  override def toString = s"virt.PrimArr(${tpe.unparse}: ${backing.fold("")(_+", "+_)})"
}





