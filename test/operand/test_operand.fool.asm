push 0
push 2
push 4
push 10
lfp
push -3
add
lw
lfp
push -2
add
lw
bleq label0
push 0
b label1
label0:
push 1
label1:
lfp
push -3
add
lw
lfp
push -2
add
lw
push 1
sub
bleq label2
push 1
b label3
label2:
push 0
label3:
lfp
push -5
add
lw
push 1
beq label8
lfp
push -6
add
lw
push 1
beq label8
push 0
b label9
label8:
push 1
label9:
push 0
beq label6
lfp
push -6
add
lw
push 0
beq label6
push 1
b label7
label6:
push 0
label7:
push 1
beq label4
lfp
push -4
add
lw
push 2
div
b label5
label4:
lfp
push -4
add
lw
lfp
push -3
add
lw
sub
label5:
print
halt