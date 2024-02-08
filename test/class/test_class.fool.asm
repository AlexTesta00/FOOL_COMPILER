push 0
push 10
bleq label2
push 0
b label3
label2:
push 1
label3:
push 1
beq label0
push 1
b label1
label0:
push 20
label1:
print
halt