package dev.konqasasas.beat.gui;

final class MenuPagination {
    static final int PAGE_SIZE=45;
    private MenuPagination(){}
    static int pages(int total){if(total<0)throw new IllegalArgumentException("total must not be negative");return Math.max(1,(total+PAGE_SIZE-1)/PAGE_SIZE);}
    static int clamp(int requested,int total){return Math.max(0,Math.min(requested,pages(total)-1));}
    static int from(int page){return page*PAGE_SIZE;}
    static int to(int page,int total){return Math.min(total,(page+1)*PAGE_SIZE);}
}
