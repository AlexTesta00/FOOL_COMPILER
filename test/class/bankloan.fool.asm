push 0
lhp

push label0
lhp
sw
lhp
push 1
add
shp
lhp

push label0
lhp
sw
lhp
push 1
add
shp
push label1
lhp
sw
lhp
push 1
add
shp
lhp

push label2
lhp
sw
lhp
push 1
add
shp
push label3
lhp
sw
lhp
push 1
add
shp
lhp

push label2
lhp
sw
lhp
push 1
add
shp
push label8
lhp
sw
lhp
push 1
add
shp
push label3
lhp
sw
lhp
push 1
add
shp


push 50000
push 40000

lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 9997
lw
lhp
sw
lhp
lhp
push 1
add
shp

lhp
sw
lhp
push 1
add
shp
push 9995
lw
lhp
sw
lhp
lhp
push 1
add
shp

push 20000
push 5000

lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 9997
lw
lhp
sw
lhp
lhp
push 1
add
shp
lfp
lfp
push -7
add
lw
lfp
push -6
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js
lfp
push -8
add
lw
push -1
beq label15
push 0
b label16
label15:
push 1
label16:
push 1
beq label13
lfp
lfp
push -8
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
b label14
label13:
push 0
label14:
print
halt

label0:
cfp
lra
lfp
lw
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

label1:
cfp
lra
lfp
lw
push -2
add
lw
stm
sra
pop
sfp
ltm
lra
js

label2:
cfp
lra
lfp
lw
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

label3:
cfp
lra
lfp
lfp
push 1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
lfp
lfp
push 1
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js
add
push 30000
push 1
sub
bleq label6
push 1
b label7
label6:
push 0
label7:
push 1
beq label4
push -1
b label5
label4:

lfp
lfp
lw
push -1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js

lhp
sw
lhp
push 1
add
shp
push 9998
lw
lhp
sw
lhp
lhp
push 1
add
shp
label5:
stm
sra
pop
pop
sfp
ltm
lra
js

label8:
cfp
lra
lfp
lfp
push 1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
push 20000
push 1
sub
bleq label11
push 1
b label12
label11:
push 0
label12:
push 1
beq label9
push -1
b label10
label9:

lfp
lfp
lw
push -1
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
lfp
lfp
lw
push -1
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js

lhp
sw
lhp
push 1
add
shp
lhp
sw
lhp
push 1
add
shp
push 9997
lw
lhp
sw
lhp
lhp
push 1
add
shp
label10:
stm
sra
pop
pop
sfp
ltm
lra
js