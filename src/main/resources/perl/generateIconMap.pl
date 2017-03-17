#!/usr/bin/perl -w

use strict;
use warnings;


if(@ARGV!=1){
	die "Usage\tIcon root\n";
}

my $iconPath=$ARGV[0];
my $themeCacheFile="$iconPath/theme.cache";
my $size='DEFAULT';
my $removePath=$iconPath;
my $iconCacheFile="$iconPath/theme.cache";
my %imageFormats=(png=>'',svg=>'',jpg=>'',gif=>'');

if(-d $iconPath){
	open(FILE,">$themeCacheFile") || die "Could not open file $themeCacheFile for writing\n";
	readDir($iconPath);
	close(FILE);
}else{
	die "$iconPath is not a directory\n";
}

sub readDir{
	my $dir=shift();
	my $lastSize=$size;
	
	my $reset=0;
	if($dir=~/([0-9]+)x[0-9]+$/){
		$size=$1;
		$removePath=$dir;
		$reset=1;
	}

	opendir(DIR,$dir) || die "Could not open the dir $dir for reading\n";
	foreach my $item(readdir(DIR)){
		my $path="$dir/$item";
		if(-d $path){
			if($item ne '.' && $item ne '..'){
				readDir($path,$size);
			}
		}else{
			if($item=~/\.([0-9a-z]+)$/i){
				my ($itemName,$extension)=($`,$1);
				if(exists $imageFormats{$extension}){
					$path=~s/\Q$removePath\E//g;
					$path=~s/^\///;
					$path=~s/\.([a-z]+)$//i;
					print FILE "$size~$extension~$path\n";
				}
			}
		}
	}
	close(DIR);
	if($reset==1){
		$removePath=$iconPath;
		$size=$lastSize;
	}
}

