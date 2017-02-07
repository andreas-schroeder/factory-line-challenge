package com.as24.factory

class FactoryService (
  itemSource: FactorySource[Item],
  ordersSource: FactorySource[Order],
  qcService: QCService,
  goodBoxesSink: FactorySink[PresentBox],
  badBoxesSink: FactorySink[PresentBox]
) extends Factory {

  // itemSource ->                                         -> goodBoxesSink
  //                 -> present boxes -> qcService.isOk ->
  // ordersSource ->                                       -> badBoxesSink

  def run(): Unit = ???

}
