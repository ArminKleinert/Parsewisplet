import de.kleinert.parsewisp.Parsewisp

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val p = Parsewisp.parser("""
                sum          = sum ('+'|'-') sum | product
                product      = power ('*'|'/') product | power
                power        = paren-or-val '^' power | paren-or-val
                paren-or-val = '(' sum ')' | number
                number       = ('+'|'-')? ('0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9')+
                """.trimIndent())
    println(p.parse("1+2*3^2/4"))
}