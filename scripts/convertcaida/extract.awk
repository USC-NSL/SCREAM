BEGIN{
	OFS=",";
}
{
	if ($1~/[0-9]+:[0-9]+.*/) {
		if ($6~/(tcp)|(UDP)/){
			#split($3,srcip,".");
			#split($5,dstip,".");
			hassrcport=match($3,/[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+\./);
			if (hassrcport){
				srcip=substr($3,1,RLENGTH-1)
				srcportindex=match($3,/[0-9]+[^0-9]*$/); 
				srcport=substr($3,srcportindex,RLENGTH)
			}else{			
				srcip=substr($3,1,RLENGTH)
				srcport="";
			}

			hasdstport=match($5,/[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+\./);
			if (hasdstport){
				dstip=substr($5,1,RLENGTH-1)
				dstportindex=match($5,/[0-9]+[^0-9]*$/); 
				dstport=substr($5,dstportindex,RLENGTH-1)
			}else{			
				dstip=substr($5,1,RLENGTH-1)#it has a collon
				dstport="";
			}

			if ($6~/tcp/){
				size=$7;
			#	print $1,substr($3,1,srcportindex-2),substr($5,1,dstportindex-2),substr($3,srcportindex,srcportlength),substr($5,dstportindex,dstportlength),$6,$7
			}else{
				size=$8
			#	print $1,substr($3,1,srcportindex-2),substr($5,1,dstportindex-2),substr($3,srcportindex,srcportlength),substr($5,dstportindex,dstportlength),substr($6,1,3),$8
			}
			print $1,srcip,dstip,srcport,dstport,substr($6,1,3),size
		}
	}
}
