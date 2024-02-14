push 0
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
push function1
push function2
push function4
push function5

push 2

push 1

push 4

push 3

push 2

push 5
push -1

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
push 9998
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
push 9998
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
push 9998
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
lfp
lfp
lfp
push -7
add
lw
lfp
stm
ltm
ltm

push -6
add
lw
js
lfp
stm
ltm
ltm

push -3
add
lw
js
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

function0:
cfp
lra

lfp
push 2
add
lw
lfp
push 1
add
lw

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
push 9998
lw
lhp
sw
lhp
lhp
push 1
add
shp
stm
sra
pop
pop
pop
sfp
ltm
lra
js

function1:
cfp
lra
push function0
lfp
push 1
add
lw
push -1
beq label4
push 0
b label5
label4:
push 1
label5:
push 1
beq label2
lfp
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
print
lfp
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
lfp
lw
stm
ltm
ltm

push -3
add
lw
js
lfp
stm
ltm
ltm

push -2
add
lw
js
b label3
label2:
push -1
label3:
stm
pop
sra
pop
pop
sfp
ltm
lra
js

function2:
cfp
lra
lfp
push 1
add
lw
push -1
beq label8
push 0
b label9
label8:
push 1
label9:
push 1
beq label6

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
push 2
add
lw
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
lfp
lw
stm
ltm
ltm

push -4
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
push 9998
lw
lhp
sw
lhp
lhp
push 1
add
shp
b label7
label6:
lfp
push 2
add
lw
label7:
stm
sra
pop
pop
pop
sfp
ltm
lra
js

function3:
cfp
lra
lfp
lw
push 3
add
lw
push 1
beq label10
lfp
push 1
add
lw
push 0
beq label12
push 0
b label13
label12:
push 1
label13:
b label11
label10:
lfp
push 1
add
lw
label11:
stm
sra
pop
pop
sfp
ltm
lra
js

function4:
cfp
lra
push function3
lfp
push 1
add
lw
push -1
beq label16
push 0
b label17
label16:
push 1
label17:
push 1
beq label14
lfp
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
push 2
add
lw
bleq label20
push 0
b label21
label20:
push 1
label21:
lfp
stm
ltm
ltm

push -2
add
lw
js
push 1
beq label18
lfp
lfp
push 3
add
lw
lfp
push 2
add
lw
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
lfp
lw
stm
ltm
ltm

push -5
add
lw
js
b label19
label18:

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
push 3
add
lw
lfp
push 2
add
lw
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
lfp
lw
stm
ltm
ltm

push -5
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
push 9998
lw
lhp
sw
lhp
lhp
push 1
add
shp
label19:
b label15
label14:
push -1
label15:
stm
pop
sra
pop
pop
pop
pop
sfp
ltm
lra
js

function5:
cfp
lra
lfp
push 1
add
lw
push -1
beq label24
push 0
b label25
label24:
push 1
label25:
push 1
beq label22
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
b label23
label22:
push 0
label23:
lfp
push 1
add
lw
push -1
beq label28
push 0
b label29
label28:
push 1
label29:
push 1
beq label26
lfp

lfp
push -2
add
lw
lfp
lfp
push 0
lfp
push -2
add
lw
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
lfp
lw
stm
ltm
ltm

push -5
add
lw
js
lfp
lw
stm
ltm
ltm

push -6
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
push 9998
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
push 1
lfp
push -2
add
lw
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
lfp
lw
stm
ltm
ltm

push -5
add
lw
js
lfp
lw
stm
ltm
ltm

push -6
add
lw
js
lfp
lw
stm
ltm
ltm

push -4
add
lw
js
b label27
label26:
push -1
label27:
stm
pop
sra
pop
pop
sfp
ltm
lra
js