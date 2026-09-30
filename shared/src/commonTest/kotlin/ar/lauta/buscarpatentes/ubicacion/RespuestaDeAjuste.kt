package ar.lauta.buscarpatentes.ubicacion

/**
 * Una respuesta real del servicio de ajuste, para las pruebas (T001 de la 007).
 *
 * La caminata es inventada, cerca del Obelisco: 13 puntos en línea recta hacia el oeste, un salto
 * de unos 680 m sin puntos, y 11 más. Se mandó **en un solo pedido**, como lo hacía la app antes de
 * la 007, porque así la respuesta trae las tres cosas que el armado tiene que resolver: un corte
 * entre pedazos (la arista 18 no empieza donde terminó la 17), un punto sin emparejar en el medio
 * (el 11) y cinco al final. Recortada a los campos que lee la app.
 */
internal val CAMINATA: List<Pair<Double, Double>> = listOf(
    -34.60365 to -58.3815,
    -34.603683 to -58.382167,
    -34.603717 to -58.382833,
    -34.60375 to -58.3835,
    -34.603783 to -58.384167,
    -34.603817 to -58.384833,
    -34.60385 to -58.3855,
    -34.603883 to -58.386167,
    -34.603917 to -58.386833,
    -34.60395 to -58.3875,
    -34.603983 to -58.388167,
    -34.604017 to -58.388833,
    -34.60405 to -58.3895,
    -34.61 to -58.391,
    -34.61002 to -58.3903,
    -34.61004 to -58.3896,
    -34.61006 to -58.3889,
    -34.61008 to -58.3882,
    -34.6101 to -58.3875,
    -34.61012 to -58.3868,
    -34.61014 to -58.3861,
    -34.61016 to -58.3854,
    -34.61018 to -58.3847,
    -34.6102 to -58.384,
)

internal val RESPUESTA: String = """
{"shape":"jc`_aAvjijnB?pA@jBJ|CHjBJlAL|ARlAn@dDxCfKTxJHrC`@p_@TtG^hOm@pGPxILvEHzE@\\nChjAFvCJnEhCvkA
LfDP~GjD`eAJnDP`EFzAPpEX`nAtDG|CIdEKt_AiBvBEjCCbaAw@lBA~ACzbAkAzACjBG`lAyDxFQBxF`Ap@hBrAtAm@xBKH|DhB
zo@fUgAfH[PzHjA`d@\\fSi@~Af@`B~@tCtBaBx@iA~@gBnBiFG{Co@eSKoDIyCgDaG{@{EGsAhAEdBIcDklACcEmCiy@SwISqKI
_D~EQ","edges":[{"begin_shape_index":0,"end_shape_index":8},{"begin_shape_index":8,"end_shape_index"
:9},{"begin_shape_index":9,"end_shape_index":10},{"begin_shape_index":10,"end_shape_index":12},{"beg
in_shape_index":12,"end_shape_index":14},{"begin_shape_index":14,"end_shape_index":16},{"begin_shape
_index":16,"end_shape_index":17},{"begin_shape_index":17,"end_shape_index":18},{"begin_shape_index":
18,"end_shape_index":20},{"begin_shape_index":20,"end_shape_index":21},{"begin_shape_index":21,"end_
shape_index":22},{"begin_shape_index":22,"end_shape_index":23},{"begin_shape_index":23,"end_shape_in
dex":24},{"begin_shape_index":24,"end_shape_index":25},{"begin_shape_index":25,"end_shape_index":26}
,{"begin_shape_index":26,"end_shape_index":27},{"begin_shape_index":27,"end_shape_index":28},{"begin
_shape_index":28,"end_shape_index":30},{"begin_shape_index":31,"end_shape_index":32},{"begin_shape_i
ndex":32,"end_shape_index":33},{"begin_shape_index":33,"end_shape_index":34},{"begin_shape_index":34
,"end_shape_index":36},{"begin_shape_index":36,"end_shape_index":39},{"begin_shape_index":39,"end_sh
ape_index":42},{"begin_shape_index":42,"end_shape_index":44},{"begin_shape_index":44,"end_shape_inde
x":45},{"begin_shape_index":45,"end_shape_index":47},{"begin_shape_index":47,"end_shape_index":48},{
"begin_shape_index":48,"end_shape_index":50},{"begin_shape_index":50,"end_shape_index":51},{"begin_s
hape_index":51,"end_shape_index":52},{"begin_shape_index":52,"end_shape_index":53},{"begin_shape_ind
ex":53,"end_shape_index":54},{"begin_shape_index":54,"end_shape_index":55},{"begin_shape_index":55,"
end_shape_index":56},{"begin_shape_index":56,"end_shape_index":58},{"begin_shape_index":58,"end_shap
e_index":60},{"begin_shape_index":60,"end_shape_index":64},{"begin_shape_index":64,"end_shape_index"
:68},{"begin_shape_index":68,"end_shape_index":71},{"begin_shape_index":71,"end_shape_index":73},{"b
egin_shape_index":73,"end_shape_index":74},{"begin_shape_index":74,"end_shape_index":78},{"begin_sha
pe_index":78,"end_shape_index":79},{"begin_shape_index":79,"end_shape_index":80}],"matched_points":[
{"type":"matched","edge_index":0},{"type":"matched","edge_index":2},{"type":"matched","edge_index":3
},{"type":"matched","edge_index":5},{"type":"matched","edge_index":8},{"type":"matched","edge_index"
:8},{"type":"matched","edge_index":11},{"type":"matched","edge_index":11},{"type":"matched","edge_in
dex":14},{"type":"matched","edge_index":14},{"type":"matched","edge_index":17},{"type":"unmatched"},
{"type":"matched","edge_index":18},{"type":"matched","edge_index":35},{"type":"matched","edge_index"
:39},{"type":"matched","edge_index":41},{"type":"matched","edge_index":42},{"type":"matched","edge_i
ndex":42},{"type":"matched","edge_index":44},{"type":"unmatched"},{"type":"unmatched"},{"type":"unma
tched"},{"type":"unmatched"},{"type":"unmatched"}]}
""".lines().joinToString("")
