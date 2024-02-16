push 0
push 1
lfp
push -2
add
lw
push 0
beq label2
push 0
b label3
label2:
push 1
label3:
push 1
beq label0
lfp
push -2
add
lw
b label1
label0:
lfp
push -2
add
lw
label1:
print
halt