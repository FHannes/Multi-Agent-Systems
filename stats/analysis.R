library("car")
library("reshape2")

#### set-up

refData <- read.table(file="output-123-ref.txt",header=T,sep=" ")

timeagent15 <- read.table(file="output-123-time-15-agent.txt",header=T,sep=" ")
timeparcel15 <- read.table(file="output-123-time-15-parcel.txt",header=T,sep=" ")

timeagent60 <- read.table(file="output-123-time-60-agent.txt",header=T,sep=" ")
timeparcel60 <- read.table(file="output-123-time-60-parcel.txt",header=T,sep=" ")

timeagent120 <- read.table(file="output-123-time-120-agent.txt",header=T,sep=" ")
timeparcel120 <- read.table(file="output-123-time-120-parcel.txt",header=T,sep=" ")

threshagent15 <- read.table(file="output-123-tres-15-agent.txt",header=T,sep=" ")
threshparcel15 <- read.table(file="output-123-tres-15-parcel.txt",header=T,sep=" ")

threshagent60 <- read.table(file="output-123-tres-60-agent.txt",header=T,sep=" ")
threshparcel60 <- read.table(file="output-123-tres-60-parcel.txt",header=T,sep=" ")

threshagent120 <- read.table(file="output-123-tres-120-agent.txt",header=T,sep=" ")
threshparcel120 <- read.table(file="output-123-tres-120-parcel.txt",header=T,sep=" ")

par(mfrow=c(1,3))
plot(density(refData$timeToDelivery))
plot(density(refData$timeToPickup))
plot(density(refData$timeOnRoute))

plot(density(timeagent15$timeToDelivery))
plot(density(timeagent15$timeToPickup))
plot(density(timeagent15$timeOnRoute))

plot(density(timeagent60$timeToDelivery))
plot(density(timeagent60$timeToPickup))
plot(density(timeagent60$timeOnRoute))

plot(density(timeagent120$timeToDelivery))
plot(density(timeagent120$timeToPickup))
plot(density(timeagent120$timeOnRoute))

plot(density(timeparcel15$timeToDelivery))
plot(density(timeparcel15$timeToPickup))
plot(density(timeparcel15$timeOnRoute))

plot(density(timeparcel60$timeToDelivery))
plot(density(timeparcel60$timeToPickup))
plot(density(timeparcel60$timeOnRoute))

plot(density(timeparcel120$timeToDelivery))
plot(density(timeparcel120$timeToPickup))
plot(density(timeparcel120$timeOnRoute))

plot(density(threshagent15$timeToDelivery))
plot(density(threshagent15$timeToPickup))
plot(density(threshagent15$timeOnRoute))

plot(density(threshagent60$timeToDelivery))
plot(density(threshagent60$timeToPickup))
plot(density(threshagent60$timeOnRoute))

plot(density(threshagent120$timeToDelivery))
plot(density(threshagent120$timeToPickup))
plot(density(threshagent120$timeOnRoute))

plot(density(threshparcel15$timeToDelivery))
plot(density(threshparcel15$timeToPickup))
plot(density(threshparcel15$timeOnRoute))

plot(density(threshparcel60$timeToDelivery))
plot(density(threshparcel60$timeToPickup))
plot(density(threshparcel60$timeOnRoute))

plot(density(threshparcel120$timeToDelivery))
plot(density(threshparcel120$timeToPickup))
plot(density(threshparcel120$timeOnRoute))

checkNormality <- function(x) {
  qqnorm(x);
  qqline(x);
  shapiro.test(x);
}

checkEqualVariance <- function(x, y, c = "median") {
  sample <- as.data.frame(cbind(x, y))
  dataset <- melt(sample)
  leveneTest(value ~ variable, dataset, c)
}

#### analysis
## compare reference to time strategy for agents
checkNormality(refData$timeToDelivery)
checkNormality(refData$timeToPickup)
checkNormality(refData$timeOnRoute)

median(refData$timeToDelivery)
median(refData$timeToPickup)
median(refData$timeOnRoute)

checkNormality(timeagent15$timeToDelivery)
wilcox.test(refData$timeToDelivery, timeagent15$timeToDelivery)
median(timeagent15$timeToDelivery)
# ! significant
wilcox.test(refData$timeToPickup, timeagent15$timeToPickup)
median(refData$timeToPickup)
median(timeagent15$timeToPickup)
# ! significant
wilcox.test(refData$timeOnRoute, timeagent15$timeOnRoute)
median(refData$timeOnRoute)
median(timeagent15$timeOnRoute)

checkNormality(timeagent60$timeToDelivery)
wilcox.test(refData$timeToDelivery, timeagent60$timeToDelivery)
median(timeagent60$timeToDelivery)
wilcox.test(refData$timeToPickup, timeagent60$timeToPickup)
median(timeagent60$timeToPickup)
# ! significant
wilcox.test(refData$timeOnRoute, timeagent60$timeOnRoute)
median(refData$timeOnRoute)
median(timeagent60$timeOnRoute)

checkNormality(timeagent120$timeToDelivery)
wilcox.test(refData$timeToDelivery, timeagent120$timeToDelivery)
median(timeagent120$timeToDelivery)
# ! significant
wilcox.test(refData$timeToPickup, timeagent120$timeToPickup)
median(refData$timeToPickup)
median(timeagent120$timeToPickup)
# ! significant
wilcox.test(refData$timeOnRoute, timeagent120$timeOnRoute)
median(refData$timeOnRoute)
median(timeagent120$timeOnRoute)

## compare reference to time strategy for parcels
checkNormality(timeparcel15$timeToDelivery)
wilcox.test(refData$timeToDelivery, timeparcel15$timeToDelivery)
median(timeparcel15$timeToDelivery)
wilcox.test(refData$timeToPickup, timeparcel15$timeToPickup)
median(timeparcel15$timeToPickup)
# ! significant
wilcox.test(refData$timeOnRoute, timeparcel15$timeOnRoute)
median(refData$timeOnRoute)
median(timeparcel15$timeOnRoute)

checkNormality(timeparcel60$timeToDelivery)
wilcox.test(refData$timeToDelivery, timeparcel60$timeToDelivery)
median(timeparcel60$timeToDelivery)
wilcox.test(refData$timeToPickup, timeparcel60$timeToPickup)
median(timeparcel60$timeToPickup)
wilcox.test(refData$timeOnRoute, timeparcel60$timeOnRoute)
median(timeparcel60$timeOnRoute)

checkNormality(timeparcel120$timeToDelivery)
# ! significant
wilcox.test(refData$timeToDelivery, timeparcel120$timeToDelivery)
median(refData$timeToDelivery)
median(timeparcel120$timeToDelivery)
wilcox.test(refData$timeToPickup, timeparcel120$timeToPickup)
median(timeparcel120$timeToPickup)
wilcox.test(refData$timeOnRoute, timeparcel120$timeOnRoute)
median(timeparcel120$timeOnRoute)

## compare reference to threshold strategy for agents
checkNormality(threshagent15$timeToDelivery)
wilcox.test(refData$timeToDelivery, threshagent15$timeToDelivery)
median(threshagent15$timeToDelivery)
wilcox.test(refData$timeToPickup, threshagent15$timeToPickup)
median(threshagent15$timeToPickup)
# ! significant
wilcox.test(refData$timeOnRoute, threshagent15$timeOnRoute)
median(refData$timeOnRoute)
median(threshagent15$timeOnRoute)

checkNormality(threshagent60$timeToDelivery)
wilcox.test(refData$timeToDelivery, threshagent60$timeToDelivery)
median(threshagent60$timeToDelivery)
wilcox.test(refData$timeToPickup, threshagent60$timeToPickup)
median(threshagent60$timeToPickup)
# ! significant
wilcox.test(refData$timeOnRoute, threshagent60$timeOnRoute)
median(refData$timeOnRoute)
median(threshagent60$timeOnRoute)

checkNormality(threshagent120$timeToDelivery)
wilcox.test(refData$timeToDelivery, threshagent120$timeToDelivery)
median(threshagent120$timeToDelivery)
wilcox.test(refData$timeToPickup, threshagent120$timeToPickup)
median(threshagent120$timeToPickup)
# ! significant
wilcox.test(refData$timeOnRoute, threshagent120$timeOnRoute)
median(refData$timeOnRoute)
median(threshagent120$timeOnRoute)

## compare reference to threshold strategy for parcels
checkNormality(threshparcel15$timeToDelivery)
# ! significant
wilcox.test(refData$timeToDelivery, threshparcel15$timeToDelivery)
median(refData$timeToDelivery)
median(threshparcel15$timeToDelivery)
wilcox.test(refData$timeToPickup, threshparcel15$timeToPickup)
median(refData$timeToPickup)
median(threshparcel15$timeToPickup)
wilcox.test(refData$timeOnRoute, threshparcel15$timeOnRoute)
median(refData$timeOnRoute)
median(threshparcel15$timeOnRoute)

checkNormality(threshparcel60$timeToDelivery)
wilcox.test(refData$timeToDelivery, threshparcel60$timeToDelivery)
median(threshparcel60$timeToDelivery)
wilcox.test(refData$timeToPickup, threshparcel60$timeToPickup)
median(threshparcel60$timeToPickup)
wilcox.test(refData$timeOnRoute, threshparcel60$timeOnRoute)
median(threshparcel60$timeOnRoute)

checkNormality(threshparcel120$timeToDelivery)
wilcox.test(refData$timeToDelivery, threshparcel120$timeToDelivery)
median(threshparcel120$timeToDelivery)
wilcox.test(refData$timeToPickup, threshparcel120$timeToPickup)
median(threshparcel120$timeToPickup)
wilcox.test(refData$timeOnRoute, threshparcel120$timeOnRoute)
median(threshparcel120$timeOnRoute)
